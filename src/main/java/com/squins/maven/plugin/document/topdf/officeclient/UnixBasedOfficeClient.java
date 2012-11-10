package com.squins.maven.plugin.document.topdf.officeclient;

import org.apache.maven.plugin.logging.Log;

public class UnixBasedOfficeClient extends AbstractOfficeClient implements OfficeClient {

    public UnixBasedOfficeClient(Log log) {
	super(log);
    }

    protected String libreOfficeExecutable() {
	return "libreoffice";
    }

    protected String libreOfficeExecutableWrapperFilenameSuffix() {
	return ".sh";
    }

    @Override
    protected String passScriptArguments() {
	return "$@";
    }

    

}
