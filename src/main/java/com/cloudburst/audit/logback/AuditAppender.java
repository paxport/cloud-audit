package com.cloudburst.audit.logback;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.model.Tracking;

import java.util.Map;
import java.util.StringJoiner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * Log Back Appender that will try auditing to the statically assigned Auditor
 */
public class AuditAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static Auditor<AuditItem> AUDITOR;

    public static void setAuditorInstance(Auditor<AuditItem> auditor) {
        AUDITOR = auditor;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if ( AUDITOR == null ) {
            return;
        }
        Level auditLevel = auditLevel();
        if (event.getLevel().isGreaterOrEqual(auditLevel)) {
            AuditItem item = createItem ( event );
            if ( item != null ) {
                AUDITOR.audit(item);
            }
        }
    }

    private AuditItem createItem(ILoggingEvent event) {

        return AuditItem.logException(
                event.getLevel().levelStr,
                event.getLoggerName(),
                event.getFormattedMessage(),
                stacktrace(event)
        );

    }

    private String stacktrace(ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if ( tp == null ) {
            return null;
        }
        StringJoiner sj = new StringJoiner("\n");
        for (StackTraceElementProxy ep : tp.getStackTraceElementProxyArray()) {
            sj.add(ep.getSTEAsString());
        }
        return sj.toString();
    }

    private Level auditLevel() {
        Map<String,String> map = Tracking.getTrackingMap();
        if ( map.containsKey(Tracking.AUDIT_LEVEL) ) {
            return Level.toLevel(map.get(Tracking.AUDIT_LEVEL),Level.WARN);
        }
        return Level.WARN;
    }
}
