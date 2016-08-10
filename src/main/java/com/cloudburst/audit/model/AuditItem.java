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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Value.Immutable
@Serial.Structural
public abstract class AuditItem implements ItemMetadata, ItemPayload, TrackingMap {

    public final static String DEFAULT_LEVEL = "INFO";

    public String timestampAsString() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(getTimestamp())
                ,ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static AuditItem log(String level, String module, String message){
        return builder()
                .type(AuditItemType.LOG)
                .level(level)
                .module(module)
                .label(message)
                .build();
    }

    public static AuditItem exception(String level, String module, String message, Throwable t){
        return builder()
                .type(AuditItemType.EXCEPTION)
                .level(level)
                .module(module)
                .label(message)
                .body(stacktrace(t))
                .build();
    }

    public static AuditItem exception(String level, String module, String message, String stacktrace){
        return builder()
                .type(AuditItemType.EXCEPTION)
                .level(level)
                .module(module)
                .label(message)
                .body(stacktrace)
                .build();
    }

    public static AuditItem request(String url, String module, String label, String headers, String requestBody, String contentType){
        return builder()
                .type(AuditItemType.REQUEST)
                .level(DEFAULT_LEVEL)
                .module(module)
                .label(label)
                .url(url)
                .headers(headers)
                .body(requestBody)
                .contentType(contentType)
                .build();
    }

    public static AuditItem response(AuditItem request, String url, String module, String label, String headers, String responseBody, String contentType){
        return builder()
                .type(AuditItemType.RESPONSE)
                .level(DEFAULT_LEVEL)
                .module(module)
                .label(label)
                .url(url)
                .headers(headers)
                .body(responseBody)
                .contentType(contentType)
                .seriesGuid(request.getGuid()) // tie response to request
                .millisTaken(System.currentTimeMillis() - request.getTimestamp())
                .build();
    }

    public static ImmutableAuditItem.Builder from (AuditItem item) {
        return ImmutableAuditItem.builder().from(item);
    }

    public static String stacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static ImmutableAuditItem.Builder builder() {
        Map<String,String> trackingMap = Tracking.getTrackingMap();
        String guid = UUID.randomUUID().toString();
        return ImmutableAuditItem.builder()
                .guid(guid)
                .seriesGuid(guid)
                .timestamp(System.currentTimeMillis())
                .hostname(getHostName())
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
