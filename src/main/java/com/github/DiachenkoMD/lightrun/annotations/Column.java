package com.github.DiachenkoMD.lightrun.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String value() default "Unnamed";
    boolean isActive() default true;
}
