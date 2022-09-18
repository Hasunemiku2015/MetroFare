package com.hasunemiku2015.metrofare.Company;

import com.hasunemiku2015.metrofare.LookUpTables.FareTables.FareTable;
import com.hasunemiku2015.metrofare.LookUpTables.FareTables.FareTableStore;

import java.io.Serializable;
import java.util.HashMap;

public class FareTableCompany extends AbstractCompany implements Serializable {
    //Serialization ID
    private static final long serialVersionUID = 314159265L;

    private transient FareTable FareTableObject;
    private final String FareTableName;

    FareTableCompany(HashMap<String, Object> input) {
        super(input);

        FareTableName = (String) input.get("faretable");
        FareTableObject = FareTableStore.FareTables.get(FareTableName);
    }

    //Getter & Setter
    public FareTable getFareTable() {
        return FareTableObject;
    }

    String getFareTableName() {
        return FareTableName;
    }
//    void setFareTable(String name) {
//        FareTableObject = FareTableStore.FareTables.get(name);
//    }

    @Override
    public int computeFare(String from, String to) {
        if(FareTableObject.getFare1000(from,to) >= 0){
            return FareTableObject.getFare1000(from,to);
        }
        return -1;
    }

    @Override
    public void onLoad() {
        FareTableObject = FareTableStore.FareTables.get(FareTableName);
    }
}
