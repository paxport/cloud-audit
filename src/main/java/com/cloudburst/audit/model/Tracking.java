package com.cloudburst.audit.model;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Serial.Structural
public abstract class Tracking implements TrackingDetails {

    private final static ThreadLocal<TrackingDetails> BOUND_ITEMS = new ThreadLocal<>();

    public static TrackingDetails of (String principal, String requestId, String tracingId, String sessionId) {
        return ImmutableTracking.builder()
                .principal(Optional.ofNullable(principal))
                .tracingId(Optional.ofNullable(tracingId))
                .requestId(Optional.ofNullable(requestId))
                .sessionId(Optional.ofNullable(sessionId))
                .build();
    }

    public static TrackingDetails of (String principal, String requestId, String tracingId,
                                      String sessionId, String correlationId) {
        return ImmutableTracking.builder()
                .principal(Optional.ofNullable(principal))
                .tracingId(Optional.ofNullable(tracingId))
                .requestId(Optional.ofNullable(requestId))
                .sessionId(Optional.ofNullable(sessionId))
                .correlationId(Optional.ofNullable(correlationId))
                .build();
    }

    public static void bind(TrackingDetails item){
        BOUND_ITEMS.set(item);
    }

    public static void bind(String principal, String requestId, String tracingId, String sessionId){
        bind(of(principal,tracingId,requestId,sessionId));
    }

    public static TrackingDetails get() {
        TrackingDetails item = BOUND_ITEMS.get();
        if ( item == null ) {
            return of(null,null,null,null);
        }
        else{
            return item;
        }
    }

    public static void unbind(){
        BOUND_ITEMS.remove();
    }
}
