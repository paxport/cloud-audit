package com.paxport.cloudaudit.logback;

import com.paxport.cloudaudit.AuditorSingleton;
import com.paxport.cloudaudit.model.AuditItem;
import com.paxport.cloudaudit.model.Tracking;

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

    @Override
    protected void append(ILoggingEvent event) {
        if ( AuditorSingleton.isPopulated() && Tracking.isBound() ) {
            Level auditLevel = auditLevel();
            if (event.getLevel().isGreaterOrEqual(auditLevel)) {
                AuditItem item = createItem(event);
                if (item != null) {
                    AuditorSingleton.getInstance().audit(item);
                }
            }
        }
    }

    private AuditItem createItem(ILoggingEvent event) {
        if ( event.getThrowableProxy() == null ) {
            return AuditItem.log(
                    event.getLevel().levelStr,
                    event.getLoggerName(),
                    event.getFormattedMessage()
            );
        }
        else {
            return AuditItem.exception(
                    event.getLevel().levelStr,
                    event.getLoggerName(),
                    event.getFormattedMessage(),
                    stacktrace(event)
            );
        }
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
