package com.cloudburst.audit.model;

import java.util.Optional;

public interface RequestResponsePair {

    Optional<String> getRequest();
    Optional<String> getResponse();
    Optional<Long> getMillisTaken();
}
