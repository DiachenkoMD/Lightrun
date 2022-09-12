package com.github.DiachenkoMD.lightrun;

import com.github.DiachenkoMD.lightrun.annotations.Order;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.Optional;

@Data
public class BenchmarkUnitResult {
    String name;
    Object result;
    Class<?> unitMethodReturnType;

    Arguments injected;
    Time time;
    Method originMethod;
    public void setNanos(long nanos){
        time = new Time();
        time.setNanos(nanos);
    }

    public Integer getOrder(){
        Optional<Order> order = Optional.ofNullable(originMethod.getAnnotation(Order.class));

        return order.isPresent() ? order.get().value() : null;
    }

    public Object getResult(){
        if(result != null)
            return result;

        if(unitMethodReturnType == void.class)
            return "void";

        return "";
    }

    @Override
    public String toString() {
        return "\n"+name+": {\n" +
                "   Output: " + result + "\n" +
                "   Return type: " + unitMethodReturnType + "\n" +
                "   Ticks: " + time.getNanos() / 100 + "\n"+
                "   Nanos: " + time.getNanos() + "\n" +
                "}\n";
    }
}
