package com.paxport.cloudaudit.model;

import com.google.api.client.util.Value;

public enum AuditItemType {

    @Value LOG,
    @Value EXCEPTION,
    @Value REQUEST,
    @Value RESPONSE,
    @Value OTHER;
}
