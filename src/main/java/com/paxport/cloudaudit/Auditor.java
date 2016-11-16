package com.paxport.cloudaudit;

public interface Auditor<E> {

    void audit (E item);
}
