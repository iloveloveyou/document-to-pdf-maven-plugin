package com.squins.maven.plugin.document.topdf.maven.officeclient;

import static java.io.File.createTempFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractOfficeClient implements OfficeClient {
    private Log log;

    private String executablePath;

    private String libreOfficeProfilePath;

    public AbstractOfficeClient(Log log) {

	this.log = log;
	createLibreOfficeExecutableWrapper();

	createLibreOfficeTemporaryUserProfileDirectory();
    }

    /**
     * Libreoffice doesn't work when called straight from Java (probably because
     * of stdout, stderr redirection). Here we create a bash wrapper to vercome
     * this issue.
     */
    private void createLibreOfficeExecutableWrapper() {
	FileWriter writer = null;
	try {
	    File file = createTempFile("document-to-pdf-maven-plugin-libre-office-executable-wrapper",
		    libreOfficeExecutableWrapperFilenameSuffix());
	    writer = new FileWriter(file);
	    writer.write(String.format("%s %s || exit 1", libreOfficeExecutable(), passScriptArguments()));
	    file.setExecutable(true);
	    file.deleteOnExit();
	    executablePath = file.getAbsolutePath();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	} finally {
	    IOUtil.close(writer);
	}
    }

    void handleProcessOutput(Process process) throws IOException {
	log.debug("Libre Office process stdout: " + IOUtil.toString(process.getInputStream()));
	String stdErrOut = IOUtil.toString(process.getErrorStream());

	if (StringUtils.isNotBlank(stdErrOut)) {
	    log.error("Libre Office process std err " + stdErrOut);
	    throw new IllegalStateException("Libreoffice execution failed: \n" + stdErrOut);
	}
    }

    public Process startAndWaitLibreProcess(String... arguments) throws Exception {
	ProcessBuilder builder = buildLibreOfficeCommand(arguments);
	Process process = builder.start();
	process.waitFor();
	handleProcessOutput(process);
	if (process.exitValue() != 0) {
	    throw new IllegalStateException("Process returned error exitValue:" + process.exitValue());
	}

	return process;

    }

    private ProcessBuilder buildLibreOfficeCommand(String... arguments) {

	List<String> commandWithArgs = new ArrayList<String>(Arrays.asList(executablePath, "--headless", "--nofirststartwizard"));

	commandWithArgs.addAll(Arrays.asList(arguments));

	commandWithArgs.add("-env:UserInstallation=file://" + libreOfficeProfilePath);

	log.info("Creating command: " + StringUtils.join(commandWithArgs.iterator(), " "));
	return new ProcessBuilder(commandWithArgs);
    }

    /**
     * Creates a temporary user profile directory.
     * <p>
     * We need to have a unique profile, since Libre office allows only one
     * concurrent process per user profile.
     */
    private void createLibreOfficeTemporaryUserProfileDirectory() {
	String path = System.getProperty("java.io.tmpdir") + "/loffice_profile" + System.nanoTime();
	final File file = new File(path);
	file.mkdir();
	libreOfficeProfilePath = file.getAbsolutePath();

	Runnable runnable = new Runnable() {

	    @Override
	    public void run() {
		try {
		    FileUtils.deleteDirectory(file);
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}
	    }
	};

	Runtime.getRuntime().addShutdownHook(new Thread(runnable));
    }
    
    protected abstract String libreOfficeExecutable();

    protected abstract String libreOfficeExecutableWrapperFilenameSuffix();
    
    protected abstract String passScriptArguments();
    
}
