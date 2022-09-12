package com.github.DiachenkoMD.lightrun;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

public class BenchmarkGroupResult extends BenchmarkUnitResult{
    List<BenchmarkUnitResult> groupedUnits = new LinkedList<>();

    public BenchmarkGroupResult(){
        super.setNanos(0);
    }

    public boolean addUnit(BenchmarkUnitResult unit){
        return groupedUnits.add(unit);
    }

    public List<BenchmarkUnitResult> getGroupedUnits() {
        return groupedUnits;
    }

    public void setGroupedUnits(List<BenchmarkUnitResult> groupedUnits) {
        this.groupedUnits = groupedUnits;
    }
}
