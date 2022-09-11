package com.github.DiachenkoMD.lightrun;

import com.github.DiachenkoMD.lightrun.annotations.Benchmark;
import com.github.DiachenkoMD.lightrun.annotations.DataSource;
import com.github.DiachenkoMD.lightrun.annotations.Unit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Lightrun {
    public static BenchmarkResults measure(Class<?> benchmarkClazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Getting benchmark annotation to acquire detailed info
        Benchmark benchmarkAnno = benchmarkClazz.getAnnotation(Benchmark.class);

        // If we passed class which is not annotated with @Benchmark -> throw exception
        if(benchmarkAnno == null)
            throw new IllegalArgumentException("Measure can only accept annotated @Benchmark classes!");

        // Forming benchmark results container, setting columns and creating new benchmarked class instance to invoke methods further
        BenchmarkResults resultsContainer = new BenchmarkResults();

        resultsContainer.setUID(benchmarkAnno.value());

        if(benchmarkAnno.colName().isActive()) resultsContainer.addColumn(BenchmarkOutputColumn.NAME, benchmarkAnno.colName().value());
        if(benchmarkAnno.colTicks().isActive()) resultsContainer.addColumn(BenchmarkOutputColumn.TICKS, benchmarkAnno.colTicks().value());
        if(benchmarkAnno.colOutput().isActive()) resultsContainer.addColumn(BenchmarkOutputColumn.OUTPUT, benchmarkAnno.colOutput().value());

        // Getting list of methods, which contains
        List<Method> unitMethods = Arrays.stream(
                benchmarkClazz.getDeclaredMethods()
        )
        .parallel()
        .filter(
                method -> method.getAnnotation(Unit.class) != null // benchmarking only methods marked with "unit"
        ).toList();

        // Warming up class and methods
        warmupBenchmark(benchmarkClazz, unitMethods);

        // Issuing real benchmarking
        Object benchmarkClass = benchmarkClazz.getConstructor().newInstance();

        unitMethods.forEach(
            method -> {
                method.setAccessible(true);

                // Getting DataSource to further decide, should we get special arguments for this method or not.
                DataSource dataSourceAnno = method.getAnnotation(DataSource.class);

                if(dataSourceAnno == null){ // simply benchmarking method without data source
                    measureMethod(method, benchmarkClass)
                            .ifPresent(resultsContainer::addResult);
                }else{
                    try {
                        // Getting source of arguments to inject
                        Method dataSourceMethod = benchmarkClazz.getDeclaredMethod(dataSourceAnno.value());
                        dataSourceMethod.setAccessible(true);

                        Stream<Arguments> dataSourceValue = (Stream<Arguments>) dataSourceMethod.invoke(benchmarkClass);

                        // Looping through stream of arguments and invoking current method with them (index needed to customize unit name)
                        AtomicInteger atomicIndex = new AtomicInteger(0);
                        dataSourceValue.forEach(
                                x -> {
                                    // Getting arguments to inject and measuring method speed
                                    Object[] injectableArgs = x.getArgs();
                                    BenchmarkUnitResult res = measureMethod(method, benchmarkClass, injectableArgs).orElse(null);

                                    if(res != null){
                                        // Making replacements (needed for customizing method name)
                                        String unitName = res.getName()
                                                .replaceAll("\\$i", String.valueOf(atomicIndex.getAndIncrement()));

                                        Parameter[] parameters = method.getParameters();

                                        int paramIndex = 0;
                                        for(Parameter param : parameters){
                                            unitName = unitName.replaceAll("\\$"+param.getName(), injectableArgs[paramIndex].toString());
                                            ++paramIndex;
                                        }

                                        // Updating method name and adding unit measuring result to container
                                        res.setName(unitName);
                                        res.setInjected(x);

                                        resultsContainer.addResult(res);
                                    }
                                }
                        );
                    } catch (NoSuchMethodException e) {
                        System.out.println("No data source with name " + dataSourceAnno.value() + " was not found! Skipped...");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );

        return resultsContainer;
    }


    private static void warmupBenchmark(Class<?> benchmarkClazz, List<Method> methods){
        try {
            Object benchmarkClass = benchmarkClazz.getConstructor().newInstance();

            for(Method method : methods) {
                method.setAccessible(true);

                // Getting DataSource to further decide, should we get special arguments for this method or not.
                DataSource dataSourceAnno = method.getAnnotation(DataSource.class);

                if (dataSourceAnno == null) { // simply warming up method without data source
                    method.invoke(benchmarkClass);
                } else {
                    // Getting source of arguments to inject
                    Method dataSourceMethod = benchmarkClazz.getDeclaredMethod(dataSourceAnno.value());
                    dataSourceMethod.setAccessible(true);

                    Stream<Arguments> dataSourceValue = (Stream<Arguments>) dataSourceMethod.invoke(benchmarkClass);

                    // Looping through stream of arguments and invoking current method with them
                    for (Arguments injectableArgs : dataSourceValue.toList()) {
                        method.invoke(benchmarkClass, injectableArgs.getArgs());
                    }
                }
            }
        }catch (Exception e){
            System.out.println(String.format("Error while warming up %s! Exception: %s", benchmarkClazz.getSimpleName(), e.getMessage()));
            e.printStackTrace();
        }
    }
    private static Optional<BenchmarkUnitResult> measureMethod(Method method, Object benchmarkClass, Object... methodArgs){
        BenchmarkUnitResult unitRes = new BenchmarkUnitResult();

        try {
            // Deciding what method to measure
            if(methodArgs.length > 0){
                long startTime = System.nanoTime();
                unitRes.setResult(method.invoke(benchmarkClass, methodArgs));
                long endTime = System.nanoTime() - startTime;
                unitRes.setNanos(endTime);
            }else{
                long startTime = System.nanoTime();
                unitRes.setResult(method.invoke(benchmarkClass));
                long endTime = System.nanoTime() - startTime;
                unitRes.setNanos(endTime);
            }


            // Adding info to current benchmarking unit
            Unit unitAnno = method.getDeclaredAnnotation(Unit.class);
            unitRes.setName(unitAnno.value());
            unitRes.setUnitMethodReturnType(method.getReturnType());

            unitRes.setOriginMethod(method);

            // Returning measuring result
            return Optional.of(unitRes);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
