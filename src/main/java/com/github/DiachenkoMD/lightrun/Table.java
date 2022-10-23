package com.github.DiachenkoMD.lightrun;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Table {
    private String name;
    private final List<ColumnConfig> columnsSettings = new LinkedList<>();
    private List<BenchmarkUnitResult> data = new LinkedList<>();
    private boolean shouldBeGrouped = false;

    private Comparator<BenchmarkUnitResult> orderBy;

    public Table(){
        setOrderBy(null);
    }

    @Override
    public String toString(){
        // Getting columns sizes
        detectColumnsSizes();

        StringBuilder tableOutput = new StringBuilder();

        // Rendering header row
        String headerRow =
                columnsSettings.stream().map(
                        columnConfig -> formCell(columnConfig.getName(), columnConfig.getSize())
                ).collect(Collectors.joining("| "));

        int headerSize = headerRow.length();

        // Rendering table name
        tableOutput.append(name).append("\n");
        // Rendering header row top border
        tableOutput.append("-".repeat(headerSize)).append("\n");

        // Rendering header row with data
        tableOutput.append(headerRow).append("\n");

        // Rendering header row bottom border
        tableOutput.append("-".repeat(headerSize)).append("\n");

        // Grouping data by method
        if(shouldBeGrouped) {
            this.data = this.data
                    .stream()
                    .collect(Collectors.groupingBy(BenchmarkUnitResult::getOriginMethod))
                    .values()
                    .parallelStream()
                    .map(x -> x.stream().collect(new UnitGroupCollector()))
                    .collect(Collectors.toList());
        }

        // Rendering table body
        tableOutput
                .append(
                        data
                                .stream()
                                .sorted(
                                        ((Comparator<BenchmarkUnitResult>) (o1, o2) -> {
                                            if (o1.getOrder() != null && o2.getOrder() != null) {
                                                return o1.getOrder() - o2.getOrder();
                                            } else {
                                                return 0;
                                            }
                                        })
                                        .thenComparing(orderBy)
                                )
                                .map(
                                        benchmarkResult -> {
                                            StringJoiner joiner = new StringJoiner("| ");

                                            columnsSettings.forEach(columnConfig -> joiner.add(formCell(columnConfig.getFunction().apply(benchmarkResult).toString(), columnConfig.getSize())));

                                            return joiner.toString();
                                        }
                                ).collect(Collectors.joining("\n"))
                ).append("\n");

        tableOutput.append("-".repeat(headerSize));

        return tableOutput.toString();
    }

    private void detectColumnsSizes(){
        columnsSettings.parallelStream().forEach(columnConfig -> {
                var calculatedValues = data.parallelStream()
                    .map(t -> columnConfig.getFunction().apply(t).toString())
                    .collect(Collectors.toCollection(LinkedList::new));

            columnConfig.setSize(getMaxColumnLength(columnConfig.getName(), calculatedValues));
        });
    }
    private static <T> int getMaxColumnLength(String placeholder, List<T> data){
        return Math.max(placeholder.length(), data
                .parallelStream()
                .mapToInt(x -> x.toString().length())
                .max().getAsInt())  + 1;
    }

    private static String formCell(String value, int size){
        return value + " ".repeat(size - value.length());
    }

    private static String capitalize(String incoming){
        if(incoming != null){
            return incoming.substring(0,1).toUpperCase() + incoming.substring(1);
        }else{
            throw new IllegalArgumentException("Unable to capitalize *null*!");
        }
    }

    public int addColumn(
            @NotNull String colName,
            @NotNull Function<BenchmarkUnitResult, ?> function
    ){
        return addColumn(colName, function, this.columnsSettings.size());
    }

    public int addColumn(
            @NotNull String colName,
            @NotNull Function<BenchmarkUnitResult, ?> function,
            int position
    ){
        ColumnConfig config = new ColumnConfig();
        config.setName(colName);
        config.setFunction(function);

        this.columnsSettings.add(position, config);

        return config.getId();
    }

    public void removeColumn(int id){
        this.columnsSettings.removeIf(config -> config.getId() == id);
    }

    public void addData(List<BenchmarkUnitResult> results){
        this.data.addAll(results);
    }

    public void setGrouping(boolean isGroupingOn){
        this.shouldBeGrouped = isGroupingOn;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setOrderBy(Comparator<BenchmarkUnitResult> comparator){
        this.orderBy = Objects.requireNonNullElseGet(comparator, () -> (o1, o2) -> (int) (o1.getTime().getTicks() - o2.getTime().getTicks()));
    }

    @Data
    private static class ColumnConfig{
        private static int lastId = 0;

        {
            id = lastId;
            ++lastId;
        }

        int id;
        String name;
        Function<BenchmarkUnitResult, ?> function;
        int size;
    }

    private static class UnitGroupCollector implements Collector<BenchmarkUnitResult, BenchmarkGroupResult, BenchmarkGroupResult>{
        @Override
        public Supplier<BenchmarkGroupResult> supplier() {
            return BenchmarkGroupResult::new;
        }

        @Override
        public BiConsumer<BenchmarkGroupResult, BenchmarkUnitResult> accumulator() {
            return (group, unitResult) -> {
                if (group.getOriginMethod() == null)
                    group.setOriginMethod(unitResult.getOriginMethod());

                if (group.getName() == null)
                    group.setName(unitResult.getName());

                group.addUnit(unitResult);
                group.setNanos(group.getTime().getNanos() + unitResult.getTime().getNanos());
            };
        }

        @Override
        public BinaryOperator<BenchmarkGroupResult> combiner() {
            return null;
        }

        @Override
        public Function<BenchmarkGroupResult, BenchmarkGroupResult> finisher() {
            return x -> x;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.UNORDERED);
        }
    }
}
