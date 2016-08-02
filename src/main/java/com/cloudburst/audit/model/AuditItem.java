package com.cloudburst.audit.model;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Serial.Structural
public abstract class AuditItem implements LogItem,ExceptionalItem, RequestResponsePair, TrackingDetails {

    public abstract String getType();

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

    public static String stacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static ImmutableAuditItem.Builder builder() {
        TrackingDetails trackingDetails = Tracking.get();
        return ImmutableAuditItem.builder()
                .timestamp(ZonedDateTime.now())
                .host(getHostName())
                .principal(trackingDetails.getPrincipal())
                .requestId(trackingDetails.getRequestId())
                .tracingId(trackingDetails.getTracingId())
                .sessionId(trackingDetails.getSessionId())
                .correlationId(trackingDetails.getCorrelationId());
    }

    public static AuditItem withTrackingInfo(AuditItem item, Map<String,String> headers) {
        return from(item)
                .principal(headers.get("principal"))
                .requestId(headers.get("requestId"))
                .tracingId(headers.get("tracingId"))
                .sessionId(headers.get("sessionId"))
                .correlationId(headers.get("correlationId"))
                .build();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "error getting host";
        }
    }


}
