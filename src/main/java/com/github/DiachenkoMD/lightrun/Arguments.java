package com.github.DiachenkoMD.lightrun;

import lombok.Data;

import java.util.Optional;

@Data
public class Arguments {
    private Object[] args;

    public static Arguments of(Object... args){
        Arguments arguments = new Arguments();
        arguments.setArgs(args);
        return arguments;
    }

    public Optional<Object> get(int index){
        if(index < args.length)
            return Optional.of(args[index]);

        return Optional.empty();
    }
}
