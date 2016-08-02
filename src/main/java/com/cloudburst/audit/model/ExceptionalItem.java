package com.cloudburst.audit.model;

import java.util.Optional;

public interface ExceptionalItem {

    Optional<String> getStacktrace();

}
