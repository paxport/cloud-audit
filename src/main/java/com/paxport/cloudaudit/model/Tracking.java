package com.paxport.cloudaudit.model;

import java.util.Hashtable;
import java.util.Map;

public abstract class Tracking {

    public final static String AUDIT_LEVEL = "audit-level";

    private final static ThreadLocal<Map<String,String>> BOUND_MAPS = new ThreadLocal<>();

    public static void bindTrackingMap(Map<String,String> trackingMap){
        BOUND_MAPS.set(trackingMap);
    }

    public static Map<String,String> getTrackingMap() {
        Map<String,String> map = BOUND_MAPS.get();
        if ( map == null ) {
            return new Hashtable<>();
        }
        else{
            return map;
        }
    }

    public static void unbindTrackingMap(){
        BOUND_MAPS.remove();
    }

    public static boolean isBound(){
        return BOUND_MAPS.get() != null;
    }
}
