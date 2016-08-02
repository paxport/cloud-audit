package com.cloudburst.audit;

public interface Auditor<E> {

    void audit (E item);
}
