package com.foo.example.dicom;

/**
 * Configuration class for DICOM server connection parameters
 */
public class DicomServerConfig {
    private String hostname;
    private int port;
    private String callingAET;  // Application Entity Title
    private String calledAET;
    private int connectTimeout;
    private int responseTimeout;

    public DicomServerConfig() {
        // Default values
        this.hostname = "localhost";
        this.port = 11112;
        this.callingAET = "WEBAPP";
        this.calledAET = "DCMSERVER";
        this.connectTimeout = 5000; // 5 seconds
        this.responseTimeout = 10000; // 10 seconds
    }

    public DicomServerConfig(String hostname, int port, String callingAET, String calledAET) {
        this();
        this.hostname = hostname;
        this.port = port;
        this.callingAET = callingAET;
        this.calledAET = calledAET;
    }

    // Getters and setters
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCallingAET() {
        return callingAET;
    }

    public void setCallingAET(String callingAET) {
        this.callingAET = callingAET;
    }

    public String getCalledAET() {
        return calledAET;
    }

    public void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }
}