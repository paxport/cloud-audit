package com.cloudburst.audit.model;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@Serial.Structural
public abstract class AuditItem implements LogItem,ExceptionalItem, RequestResponsePair, TrackingMap {

    public abstract String getType();

    public abstract String getGuid();

    public static AuditItem logItem(String level, String module, String message){
        return builder()
                .type("LOG")
                .level(level)
                .module(module)
                .message(message)
                .build();
    }

    public static AuditItem logException(String level, String module, String message, Throwable t){
        return builder()
                .type("LOG")
                .level(level)
                .module(module)
                .message(message)
                .stacktrace(stacktrace(t))
                .build();
    }

    public static AuditItem logException(String level, String module, String message, String stacktrace){
        return builder()
                .type("LOG")
                .level(level)
                .module(module)
                .message(message)
                .stacktrace(Optional.ofNullable(stacktrace))
                .build();
    }

    public static AuditItem requestResponse(long requestTime, String level, String module, String message,
                                            String request, String response, Long millisTaken){
        return builder()
                .timestamp(ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(requestTime), ZoneId.systemDefault())
                )
                .type("RRPAIR")
                .level(level)
                .module(module)
                .message(message)
                .request(request)
                .response(Optional.ofNullable(response))
                .millisTaken(Optional.ofNullable(millisTaken))
                .build();
    }

    public static AuditItem requestResponse(String level, String module, String message,
                                            String request, String response, Long millisTaken){
        return builder()
                .type("RRPAIR")
                .level(level)
                .module(module)
                .message(message)
                .request(request)
                .response(Optional.ofNullable(response))
                .millisTaken(Optional.ofNullable(millisTaken))
                .build();
    }

    public static ImmutableAuditItem.Builder from (AuditItem item) {
        return ImmutableAuditItem.builder().from(item);
    }

    public static Optional<String> stacktrace(Throwable t) {
        if ( t == null ) {
            return Optional.empty();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return Optional.of(sw.toString());
    }

    public static ImmutableAuditItem.Builder builder() {
        Map<String,String> trackingMap = Tracking.getTrackingMap();
        return ImmutableAuditItem.builder()
                .guid(UUID.randomUUID().toString())
                .timestamp(ZonedDateTime.now())
                .host(getHostName())
                .tracking(trackingMap);
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "error getting host";
        }
    }

}
