package com.squins.maven.plugin.document.topdf.maven.officeclient;

import org.apache.maven.plugin.logging.Log;

public class WindowsOfficeClient extends AbstractOfficeClient {

    public WindowsOfficeClient(Log log) {
	super(log);
    }

    @Override
    protected String libreOfficeExecutable() {
	return "\"C:\\Program Files\\LibreOffice 3.6\\program\\soffice.exe\"";
    }

    @Override
    protected String libreOfficeExecutableWrapperFilenameSuffix() {
	return ".bat";
    }
    
    @Override
    protected String passScriptArguments() {
        return "%1 %2 %3 %4 %5 %6 %7 %8 %9";
    }

}
