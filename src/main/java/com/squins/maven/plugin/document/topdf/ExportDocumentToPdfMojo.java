package com.squins.maven.plugin.document.topdf;

import static java.io.File.createTempFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

@Mojo(name = "export")
public class ExportDocumentToPdfMojo extends AbstractMojo {

    private String executablePath;

    private String libreOfficeProfilePath;

    static final String LIBRE_OFFICE_DEFAULT_EXECUTABLE = "libreoffice";

    final String libreOfficeExecutable;

    List<String> includedExtensions = Arrays.asList("odt", "doc", "docx");

    @Parameter(property = "documentDirectory", required = true, defaultValue = "${project.basedir}/src/main/documents")
    File documentDirectory;

    @Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.outputDirectory}")
    File outputDirectory;

    public ExportDocumentToPdfMojo() {
	this(LIBRE_OFFICE_DEFAULT_EXECUTABLE);
    }

    /**
     * For the Maven plugin, which injects required properties.
     * 
     * @param libreOfficeExecutable
     */
    public ExportDocumentToPdfMojo(String libreOfficeExecutable) {
	this.libreOfficeExecutable = libreOfficeExecutable;

	failOnWindows();
	createLibreOfficeExecutableWrapper();
	createLibreOfficeTemporaryUserProfileDirectory();
    }

    public ExportDocumentToPdfMojo(File documentDirectory, File outputDirectory, String libreOfficeExecutable) {
	this(libreOfficeExecutable);

	this.documentDirectory = documentDirectory;
	this.outputDirectory = outputDirectory;
    }

    private void assertLibreOfficeAvailable() throws MojoFailureException {
	try {
	    startAndWaitLibreProcess("--help");
	} catch (Exception e) {
	    throw new MojoFailureException("Failed to start libreoffice; is libreoffice executable on your os path?", e);
	}
    }

    private ProcessBuilder buildLibreOfficeCommand(String... arguments) {

	List<String> commandWithArgs = new ArrayList<String>(Arrays.asList(executablePath, "--headless"));

	commandWithArgs.addAll(Arrays.asList(arguments));

	commandWithArgs.add("-env:UserInstallation=file://" + libreOfficeProfilePath);

	getLog().info("Creating command: " + StringUtils.join(commandWithArgs.iterator(), " "));
	return new ProcessBuilder(commandWithArgs);
    }

    /**
     * Libreoffice doesn't work when called straight from Java (probably because
     * of stdout, stderr redirection). Here we create a bash wrapper to vercome
     * this issue.
     */
    private void createLibreOfficeExecutableWrapper() {
	FileWriter writer = null;
	try {
	    File file = createTempFile("DocumentToPdfPluginMojo", "libreofficeexecutableWrapper");
	    writer = new FileWriter(file);
	    writer.write(String.format("%s $@ || exit 1", libreOfficeExecutable));
	    file.setExecutable(true);
	    file.deleteOnExit();
	    executablePath = file.getAbsolutePath();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	} finally {
	    IOUtil.close(writer);
	}
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

    void handleProcessOutput(Process process) throws IOException {
	getLog().debug("Libre Office process stdout: " + IOUtil.toString(process.getInputStream()));
	String stdErrOut = IOUtil.toString(process.getErrorStream());

	if (StringUtils.isNotBlank(stdErrOut)) {
	    getLog().error("Libre Office process std err " + stdErrOut);
	    throw new IllegalStateException("Libreoffice execution failed: \n" + stdErrOut);
	}
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	assertLibreOfficeAvailable();
	getLog().info("documentDirectory: " + documentDirectory);
	getLog().info("outputDirectory: " + outputDirectory);

	DirectoryScanner scanner = new DirectoryScanner();
	scanner.setBasedir(documentDirectory);
	scanner.setIncludes(makeIncludesOfExtensions());
	scanner.scan();
	String[] includedFiles = scanner.getIncludedFiles();
	try {
	    for (String includedFile : includedFiles) {
		exportToPdf(includedFile);
	    }
	} catch (Exception e) {
	    throw new MojoFailureException("Failed to perform conversion.", e);
	}
    }

    private void exportToPdf(String relativeFilePath) throws Exception {

	File inputFile = new File(documentDirectory.getAbsolutePath() + File.separator + relativeFilePath);

	File documentOutputDir = determinteOutputDir(relativeFilePath);

	startAndWaitLibreProcess(
	/* */
	"--convert-to pdf",
	/* */
	inputFile.getAbsolutePath(),
	/* */
	"--outdir", documentOutputDir.getAbsolutePath());
    }

    private File determinteOutputDir(String relativeDocumentFilePath) {
	File currentDocumentDirectory = new File(documentDirectory, relativeDocumentFilePath).getParentFile();
	if (documentDirectory.equals(currentDocumentDirectory)) {
	    return outputDirectory;
	}
	String relativeDocumentDir = currentDocumentDirectory.getAbsolutePath().substring(
		documentDirectory.getAbsolutePath().length());
	return new File(outputDirectory, relativeDocumentDir);
    }

    private String[] makeIncludesOfExtensions() {
	List<String> includes = new ArrayList<String>(this.includedExtensions);
	for (String includedExtension : includedExtensions) {
	    includes.add("**/*." + includedExtension);
	}

	String[] ret = new String[includes.size()];
	return includes.toArray(ret);
    }

    private void failOnWindows() {
	if ("Windows".equalsIgnoreCase(System.getProperty("os.name"))) {
	    throw new RuntimeException("Sorry this tool doesn't work on Windows yet, please vote on the Github issue");
	}
    }

    Process startAndWaitLibreProcess(String... arguments) throws Exception {
	ProcessBuilder builder = buildLibreOfficeCommand(arguments);
	Process process = builder.start();
	process.waitFor();
	handleProcessOutput(process);
	if (process.exitValue() != 0) {
	    throw new IllegalStateException("Process returned error exitValue:" + process.exitValue());
	}

	return process;
    }
}
