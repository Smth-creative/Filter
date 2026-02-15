package ru.shift.filter.statsModes;

import ru.shift.filter.ValueType;

import java.io.PrintStream;

public class ShortStats implements StatsModeStrategy {
    private long intCount, floatCount, stringCount;

    @Override
    public void accept(ValueType type, String rawLine) {
        switch (type) {
            case INTEGER -> intCount++;
            case FLOAT -> floatCount++;
            case STRING -> stringCount++;
        }
    }

    @Override
    public void print(PrintStream out) {
        out.println("Integers written: " + intCount);
        out.println("Floats written:   " + floatCount);
        out.println("Strings written:  " + stringCount);
    }
}
