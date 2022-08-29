package io.kontur.eventapi.entity;

public enum Severity {
    EXTREME(5),
    SEVERE(4),
    MODERATE(3),
    MINOR(2),
    TERMINATION(1),
    UNKNOWN(0);

    private int value;

    Severity(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }
}
