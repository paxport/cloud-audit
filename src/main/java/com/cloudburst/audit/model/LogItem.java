package com.cloudburst.audit.model;

import java.time.ZonedDateTime;


public interface LogItem {

    String getModule();
    String getHost();
    String getLevel();
    ZonedDateTime getTimestamp();
    String getMessage();

}
