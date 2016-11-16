package com.paxport.cloudaudit.model;

import javax.annotation.Nullable;

public interface ItemMetadata {

    AuditItemType getType();

    String getGuid();

    long getTimestamp();

    String getLevel();

    String getModule();

    String getLabel();

    String getHostname();

    @Nullable
    String getUrl();

    @Nullable
    String getSeriesGuid();

    @Nullable
    Long getMillisTaken();
}
