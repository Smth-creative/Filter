package ru.shift.filter.statsModes;

import ru.shift.filter.ValueType;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.IntSummaryStatistics;

public class FullStats implements StatsModeStrategy {
    private BigInteger minInt = null;
    private BigInteger maxInt = null;
    private BigInteger sumInt = BigInteger.ZERO;
    private long countInt = 0;

    private BigDecimal minFloat = null;
    private BigDecimal maxFloat = null;
    private BigDecimal sumFloat = BigDecimal.ZERO;
    private long countFloat = 0;

    private final IntSummaryStatistics stringLenStats = new IntSummaryStatistics();
    private long countString = 0;

    @Override
    public void accept(ValueType type, String rawLine) {
        if (rawLine == null) return;

        switch (type) {
            case INTEGER -> updateInt(rawLine.strip());
            case FLOAT   -> updateFloat(rawLine.strip());
            case STRING  -> updateString(rawLine);
        }
    }

    private void updateInt(String line) {
        try {
            BigInteger val = new BigInteger(line);
            if (minInt == null || val.compareTo(minInt) < 0) minInt = val;
            if (maxInt == null || val.compareTo(maxInt) > 0) maxInt = val;
            sumInt = sumInt.add(val);
            countInt++;
        } catch (NumberFormatException e) {
            System.err.println("Error while parsing integer: [" + line + "] This number will not appear in statistics\n" + e);
        }
    }

    private void updateFloat(String line) {
        try {
            BigDecimal val = new BigDecimal(line);
            if (minFloat == null || val.compareTo(minFloat) < 0) minFloat = val;
            if (maxFloat == null || val.compareTo(maxFloat) > 0) maxFloat = val;
            sumFloat = sumFloat.add(val);
            countFloat++;
        } catch (NumberFormatException e) {
            System.err.println("Error while parsing float: [" + line + "] This number will not appear in statistics\n" + e);
        }
    }

    private void updateString(String line) {
        countString++;
        stringLenStats.accept(line.length());
    }

    @Override
    public void print(PrintStream out) {
        printInts(out);
        printFloats(out);
        printStrings(out);
    }

    private void printInts(PrintStream out) {
        if (countInt == 0) {
            out.println("Integers: no data");
            return;
        }

        out.println("Integers:");
        out.println("  Count = " + countInt);
        out.println("  Min   = " + minInt);
        out.println("  Max   = " + maxInt);
        out.println("  Sum   = " + sumInt);

        BigDecimal avg = new BigDecimal(sumInt).divide(BigDecimal.valueOf(countInt), MathContext.DECIMAL128);
        out.println("  Avg   = " + avg);
    }

    private void printFloats(PrintStream out) {
        if (countFloat == 0) {
            out.println("Floats: no data");
            return;
        }

        out.println("Floats:");
        out.println("  Count = " + countFloat);
        out.println("  Min   = " + minFloat);
        out.println("  Max   = " + maxFloat);
        out.println("  Sum   = " + sumFloat);

        BigDecimal avg = sumFloat.divide(BigDecimal.valueOf(countFloat), MathContext.DECIMAL128);
        out.println("  Avg   = " + avg);
    }

    private void printStrings(PrintStream out) {
        if (countString == 0) {
            out.println("Strings: no data");
            return;
        }

        out.println("Strings:");
        out.println("  Count     = " + countString);
        out.println("  MinLength = " + stringLenStats.getMin());
        out.println("  MaxLength = " + stringLenStats.getMax());
    }
}
