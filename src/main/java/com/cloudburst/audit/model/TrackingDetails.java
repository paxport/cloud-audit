package com.cloudburst.audit.model;

import java.util.Optional;


public interface TrackingDetails {

    Optional<String> getPrincipal();

    Optional<String> getRequestId();
    Optional<String> getTracingId();
    Optional<String> getSessionId();
    Optional<String> getCorrelationId();

}
