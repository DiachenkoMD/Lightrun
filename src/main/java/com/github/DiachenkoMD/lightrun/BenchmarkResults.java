package com.github.DiachenkoMD.lightrun;

import lombok.Data;

import java.util.*;
import java.util.function.Function;

@Data
public class BenchmarkResults {
    String UID;
    Map<BenchmarkOutputColumn, String> activeColumns;
    List<BenchmarkUnitResult> results;

    public boolean addResult(BenchmarkUnitResult res){
        if(results == null)
            results = new LinkedList<>();

        return results.add(res);
    }

    @Override
    public String toString() {
        return "Measure results: \n" +
                "UID: '" + UID + "'\n" +
                "Results: \n" + results + "\n";
    }

    public Table asTable(){
        Table table = new Table();

        for(Map.Entry<BenchmarkOutputColumn, String> entry : activeColumns.entrySet()){
            Function<BenchmarkUnitResult, ?> func = null;

            switch (entry.getKey()){
                case NAME -> func = BenchmarkUnitResult::getName;
                case TICKS -> func = t -> t.getTime().getTicks();
                case OUTPUT -> func = BenchmarkUnitResult::getResult;
            }

            if(func != null)
                table.addColumn(entry.getValue(), func);
        }

        table.addData(results);

        return table;
    }

    public void addColumn(BenchmarkOutputColumn colType, String colName){
        if(this.activeColumns == null)
            this.activeColumns = new LinkedHashMap<>();

        this.activeColumns.put(colType, colName);
    }
}
