package com.github.DiachenkoMD.lightrun;

import lombok.Data;

@Data
public class Time {
    long nanos;

    public long getTicks(){
        return nanos / 100;
    }
}
