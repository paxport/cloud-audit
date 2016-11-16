package com.paxport.cloudaudit;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Value.Style(
        get = {"is*", "get*"},
        depluralize = true
)
@Serial.Structural
public @interface UnimodelStyle {
}
