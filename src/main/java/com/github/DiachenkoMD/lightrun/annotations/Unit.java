package com.github.DiachenkoMD.lightrun.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Unit {
    String value();
}
