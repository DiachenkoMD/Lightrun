package com.github.DiachenkoMD.lightrun;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Table {
    private final List<ColumnConfig> columnsSettings = new LinkedList<>();
    private final List<BenchmarkUnitResult> data = new LinkedList<>();

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
        tableOutput.append("#TABLE").append("\n");
        // Rendering header row top border
        tableOutput.append("-".repeat(headerSize)).append("\n");

        // Rendering header row with data
        tableOutput.append(headerRow).append("\n");

        // Rendering header row bottom border
        tableOutput.append("-".repeat(headerSize)).append("\n");

        // Rendering table body
        tableOutput
                .append(
                        data
                                .stream()
                                .sorted(
                                        (br_1, br_2) ->
                                                (br_1.getOrder() != null && br_2.getOrder() != null)
                                                        ?
                                                        br_1.getOrder()- br_2.getOrder()
                                                        :
                                                        (int) (br_1.getTime().getTicks() - br_2.getTime().getTicks())
                                ).map(
                                        benchmarkResult -> {
                                            StringJoiner joiner = new StringJoiner("| ");

                                            columnsSettings.forEach(columnConfig -> joiner.add(formCell(columnConfig.getFunction().apply(benchmarkResult).toString(), columnConfig.getSize())));

                                            return joiner.toString();
                                        }
                                ).collect(Collectors.joining("\n"))
                );

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
}
