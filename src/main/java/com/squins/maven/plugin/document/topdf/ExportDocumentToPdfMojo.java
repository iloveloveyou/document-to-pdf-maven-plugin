package com.squins.maven.plugin.document.topdf;

import java.io.File;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.squins.maven.plugin.document.topdf.officeclient.OfficeClient;
import com.squins.maven.plugin.document.topdf.officeclient.UnixBasedOfficeClient;
import com.squins.maven.plugin.document.topdf.officeclient.WindowsOfficeClient;

@Mojo(name = "export")
public class ExportDocumentToPdfMojo extends AbstractMojo {

	List<String> includedExtensions = Arrays.asList("odt", "doc", "docx");

	@Parameter(property = "documentDirectory", required = true, defaultValue = "${project.basedir}/src/main/documents")
	File documentDirectory;

	@Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.outputDirectory}")
	File outputDirectory;

	OfficeClient officeClient;

	public ExportDocumentToPdfMojo() {
		this(null);
	}

	void determineOfficeClientIfNeeded() {
		if (officeClient != null) {
			return;
		}

		if (isOsWindows()) {
			officeClient = new WindowsOfficeClient(getLog());
			return;
		}
		officeClient = new UnixBasedOfficeClient(getLog());
	}

	private static boolean isOsWindows() {
		return StringUtils.defaultString(System.getProperty("os.name"))
				.toLowerCase().contains("windows");
	}

	/**
	 * For the Maven plugin, which injects required properties.
	 * 
	 * @param libreOfficeExecutable
	 * @param officeClient
	 */
	public ExportDocumentToPdfMojo(OfficeClient officeClient) {
		this.officeClient = officeClient;
	}

	public ExportDocumentToPdfMojo(File documentDirectory,
			File outputDirectory, OfficeClient officeClient) {
		this(officeClient);
		this.documentDirectory = documentDirectory;
		this.outputDirectory = outputDirectory;
	}

	public ExportDocumentToPdfMojo(File documentDirectory, File outputDirectory) {
		this(null);

		this.documentDirectory = documentDirectory;
		this.outputDirectory = outputDirectory;
	}

	private void assertLibreOfficeAvailable() throws MojoFailureException {
		try {
			officeClient.startAndWaitLibreProcess("--version");
		} catch (Exception e) {
			throw new MojoFailureException(
					"Failed to start libreoffice; this could be caused by that you are using Windows, or Libreoffice is not installed.",
					e);
		}
	}

	void handleProcessOutput(Process process) throws IOException {
		getLog().debug(
				"Libre Office process stdout: "
						+ IOUtil.toString(process.getInputStream()));
		String stdErrOut = IOUtil.toString(process.getErrorStream());

		if (StringUtils.isNotBlank(stdErrOut)) {
			getLog().error("Libre Office process std err " + stdErrOut);
			throw new IllegalStateException("Libreoffice execution failed: \n"
					+ stdErrOut);
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		determineOfficeClientIfNeeded();
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

		File inputFile = new File(documentDirectory.getAbsolutePath()
				+ File.separator + relativeFilePath);

		File documentOutputDir = determinteOutputDir(relativeFilePath);

		officeClient.startAndWaitLibreProcess(
		/* */
		"--convert-to", "pdf",
		/* */
		inputFile.getAbsolutePath(),
		/* */
		"--outdir", documentOutputDir.getAbsolutePath());
	}

	private File determinteOutputDir(String relativeDocumentFilePath) {
		File currentDocumentDirectory = new File(documentDirectory,
				relativeDocumentFilePath).getParentFile();
		if (documentDirectory.equals(currentDocumentDirectory)) {
			return outputDirectory;
		}
		String relativeDocumentDir = currentDocumentDirectory.getAbsolutePath()
				.substring(documentDirectory.getAbsolutePath().length());
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
}
