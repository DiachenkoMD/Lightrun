package com.github.DiachenkoMD.lightrun.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Benchmark {
    String value();
    Column colName() default @Column("Name");
    Column colTicks() default @Column("Ticks");
    Column colOutput() default @Column("Result");
}
