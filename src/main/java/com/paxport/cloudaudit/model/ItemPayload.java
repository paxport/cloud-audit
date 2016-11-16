package com.paxport.cloudaudit.model;

import javax.annotation.Nullable;

public interface ItemPayload {

    @Nullable
    String getHeaders();

    @Nullable
    String getBody();

    @Nullable
    String getContentType();
}
