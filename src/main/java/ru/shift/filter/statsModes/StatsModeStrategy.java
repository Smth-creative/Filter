package ru.shift.filter.statsModes;

import ru.shift.filter.ValueType;

import java.io.PrintStream;

public interface StatsModeStrategy {
    void accept(ValueType type, String rawLine);

    void print(PrintStream out);
}
