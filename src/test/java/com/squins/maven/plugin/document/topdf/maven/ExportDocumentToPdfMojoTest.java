package com.squins.maven.plugin.document.topdf.maven;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.squins.maven.plugin.document.topdf.maven.officeclient.OfficeClient;
import com.squins.maven.plugin.document.topdf.maven.officeclient.UnixBasedOfficeClient;
import com.squins.maven.plugin.document.topdf.maven.officeclient.WindowsOfficeClient;


public class ExportDocumentToPdfMojoTest {

    private File documentDirectory;
    private File outputDirectory;

    
    private static String realOs = System.getProperty("os.name");
    
    @Before
    public void setUp() throws Exception {
	documentDirectory = getTestDataDirectoryFile();

	outputDirectory = new File(System.getProperty("java.io.tmpdir") + "/" + System.nanoTime());
	outputDirectory.mkdir();
	System.setProperty("os.name", realOs);
    }
    
    

    @After
    public void tearDown() throws IOException {
	FileUtils.deleteDirectory(outputDirectory);
	System.setProperty("os.name", realOs);
    }

    @Test
    public void convertsKnownTypes() throws Exception {
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory).execute();

	assertTrue(new File(outputDirectory, "libre-office.pdf").exists());
	assertTrue(new File(outputDirectory, "microsoft-office.pdf").exists());
	assertTrue(new File(outputDirectory, "subfolder/libre-office-sub-folder.pdf").exists());
    }

    @Test(expected = MojoFailureException.class)
    public void conversionFailsIfOutputDirectoryIsNotWritable() throws Exception {
	outputDirectory.setWritable(false);
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory).execute();
	assertFalse(new File(outputDirectory, "microsoft-office.pdf").exists());
    }
    
    @Test
    public void shouldChooseWindowsOfficeClientIfOsIsWindows() {
	System.setProperty("os.name", "Windows");
	ExportDocumentToPdfMojo mojo = new ExportDocumentToPdfMojo();
	mojo.determineOfficeClientIfNeeded();
	assertThat(mojo.officeClient, instanceOf(WindowsOfficeClient.class));
    }

    @Test
    public void shouldChooseUnixBasedOfficeClientIfOsIsMacOs() {
	System.setProperty("os.name", "MacOS");
	ExportDocumentToPdfMojo mojo = new ExportDocumentToPdfMojo();
	mojo.determineOfficeClientIfNeeded();
	assertThat(mojo.officeClient, instanceOf(UnixBasedOfficeClient.class));
    }

    
    @Test(expected = MojoFailureException.class)
    public void stopsExecutionIfLibreOfficeExecutableIsNotFound() throws MojoExecutionException, MojoFailureException {
	OfficeClient officeClient = new OfficeClient() {
	    
	    @Override
	    public Process startAndWaitLibreProcess(String... arguments) throws Exception {
		throw new RuntimeException("libreoffice.exe: no such command or program installed");
	    }
	};
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory, officeClient).execute();
    }

    private File getTestDataDirectoryFile() throws MalformedURLException, URISyntaxException {
	File file = new File(getClass().getResource("/test-data/libre-office.odt").toURI().toURL().getFile());
	return file.getParentFile();
    }
}
