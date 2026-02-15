package ru.shift.filter;

import picocli.CommandLine;
import ru.shift.filter.statsModes.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@CommandLine.Command(
        name = "filter",
        version = "1.0.0",
        mixinStandardHelpOptions = true,
        description = "Filters mixed file contents into integers/floats/strings output files."
)
public class FilterApp implements Callable<Integer> {
    private static final Pattern INT_PATTERN = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?$");

    @CommandLine.Option(names = "-o", description = "Output directory (default: current).", paramLabel = "<filepath>")
    private Path outDir = Path.of(".");

    @CommandLine.Option(names = "-p", description = "Prefix for all output files (default: none).", paramLabel = "<string>")
    private String prefix = "";

    @CommandLine.Option(names = "-a", description = "Append mode (default: overwrite).")
    private boolean append;

    static class StatsMode {
        @CommandLine.Option(names = "-s", description = "Short statistics mode.")
        boolean shortStats;

        @CommandLine.Option(names = "-f", description = "Full statistics mode.")
        boolean fullStats;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
    private StatsMode statsMode;

    private StatsModeStrategy statsStrategy;

    @CommandLine.Parameters(arity = "1..*", description = "Input files.", paramLabel = "<filepath>")
    private List<Path> inputFiles;

    @Override
    public Integer call() {
        statsStrategy = resolveStatsMode();

        boolean hadErrors = false;
        Throwable fatalOutputError = null;

        try (OutputManager out = new OutputManager(outDir, prefix, append)) {
            for (Path input : inputFiles) {
                try {
                    hadErrors |= processOneFile(input, out);
                } catch (UncheckedIOException e) {
                    fatalOutputError = (e.getCause() != null) ? e.getCause() : e;
                    break;
                }
            }
        } catch (IOException | RuntimeException e) {
            fatalOutputError = e;
        } finally {
            statsStrategy.print(System.out);
        }

        if (fatalOutputError != null) {
            if (fatalOutputError instanceof AccessDeniedException ade) {
                System.err.println("No rights to write: " + ade.getFile());
            } else {
                System.err.println("Fatal output error: " + fatalOutputError.getMessage());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }

        return hadErrors ? CommandLine.ExitCode.SOFTWARE : CommandLine.ExitCode.OK;
    }

    private boolean processOneFile(Path input, OutputManager out) {
        boolean hadErrors = false;

        try (var br = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                ValueType valueType = classifyLine(line);

                boolean written = out.write(valueType, line);
                if (written) {
                    statsStrategy.accept(valueType, line);
                } else {
                    hadErrors = true;
                }
            }

            return hadErrors;
        } catch (NoSuchFileException e) {
            System.err.println("No such file: " + input);
            return true;
        } catch (AccessDeniedException e) {
            System.err.println("No rights to open this file: " + input);
            return true;
        } catch (IOException e) {
            System.err.println("Unexpected error while working with file: " + input + "\n" + e.getMessage());
            return true;
        }
    }

    private StatsModeStrategy resolveStatsMode() {
        if (statsMode != null) {
            if (statsMode.shortStats) return new ShortStats();
            if (statsMode.fullStats) return new FullStats();
        }
        return new NoStats();
    }

    private ValueType classifyLine(String line) {
        String s = line.strip();

        if (s.isEmpty()) return ValueType.STRING;
        if (INT_PATTERN.matcher(s).matches()) return ValueType.INTEGER;
        if (FLOAT_PATTERN.matcher(s).matches()) return ValueType.FLOAT;

        return ValueType.STRING;
    }
}