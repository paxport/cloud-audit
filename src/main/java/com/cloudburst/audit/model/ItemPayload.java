package com.cloudburst.audit.model;

import javax.annotation.Nullable;

public interface ItemPayload {

    @Nullable
    String getHeaders();

    @Nullable
    String getBody();

    @Nullable
    String getContentType();
}
