package com.squins.maven.plugin.document.topdf.maven;

import static org.junit.Assert.assertFalse;
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

import com.squins.maven.plugin.document.topdf.ExportDocumentToPdfMojo;

public class ExportDocumentToPdfMojoTest {

    private File documentDirectory;
    private File outputDirectory;

    private String libreOfficeExecutable;

    @Before
    public void setUp() throws Exception {
	documentDirectory = getTestDataDirectoryFile();

	outputDirectory = new File(System.getProperty("java.io.tmpdir") + "/" + System.nanoTime());
	outputDirectory.mkdir();
	libreOfficeExecutable = "libreoffice";
    }

    @After
    public void tearDown() throws IOException {
	FileUtils.deleteDirectory(outputDirectory);
    }

    @Test
    public void convertsKnownTypes() throws Exception {
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory, libreOfficeExecutable).execute();

	assertTrue(new File(outputDirectory, "libre-office.pdf").exists());
	assertTrue(new File(outputDirectory, "microsoft-office.pdf").exists());
	assertTrue(new File(outputDirectory, "subfolder/libre-office-sub-folder.pdf").exists());
    }

    @Test(expected = MojoFailureException.class)
    public void conversionFailsIfOutputDirectoryIsNotWritable() throws Exception {
	outputDirectory.setWritable(false);
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory, libreOfficeExecutable).execute();
	assertFalse(new File(outputDirectory, "microsoft-office.pdf").exists());
    }

    @Test(expected = MojoFailureException.class)
    public void stopsExecutionIfLibreOfficeExecutableIsNotFound() throws MojoExecutionException, MojoFailureException {
	new ExportDocumentToPdfMojo(documentDirectory, outputDirectory, "thisExecutableDoesNotExists").execute();
    }

    private File getTestDataDirectoryFile() throws MalformedURLException, URISyntaxException {
	File file = new File(getClass().getResource("/test-data/libre-office.odt").toURI().toURL().getFile());
	return file.getParentFile();
    }
}
