package ru.shift.filter.statsModes;

import ru.shift.filter.ValueType;

import java.io.PrintStream;

public class NoStats implements StatsModeStrategy {
    @Override
    public void accept(ValueType type, String rawLine) {
        // Does nothing
    }

    @Override
    public void print(PrintStream out) {
        // Does nothing
    }
}
