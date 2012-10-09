package com.squins.maven.plugin.document.topdf.maven.officeclient;

public interface OfficeClient {

    Process startAndWaitLibreProcess(String... arguments) throws Exception; 
}
