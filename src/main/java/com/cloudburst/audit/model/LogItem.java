package com.cloudburst.audit.model;

public interface LogItem {

    String getModule();
    String getHost();
    String getLevel();
    long getTimestamp();
    String getMessage();

}
