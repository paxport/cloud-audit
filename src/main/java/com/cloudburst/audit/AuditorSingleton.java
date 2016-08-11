package com.cloudburst.audit;

import com.cloudburst.audit.model.AuditItem;

/**
 * Singleton Auditor for use in Log handlers etc
 */
public class AuditorSingleton {

    // default to no op auditor
    private static Auditor<AuditItem> AUDITOR = null;

    public static void setInstance(Auditor<AuditItem> auditor) {
        AUDITOR = auditor;
    }

    public static Auditor<AuditItem> getInstance(){
        return AUDITOR;
    }

    public static boolean isPopulated() {
        return AUDITOR != null;
    }
}
