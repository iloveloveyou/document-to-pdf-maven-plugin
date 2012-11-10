package com.squins.maven.plugin.document.topdf.officeclient;

public interface OfficeClient {

    Process startAndWaitLibreProcess(String... arguments) throws Exception; 
}
