package ru.shift.filter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class OutputManager implements AutoCloseable {
    private final Path outDir;
    private final String prefix;
    private final boolean append;

    private final Map<ValueType, BufferedWriter> writers = new EnumMap<>(ValueType.class);
    private final Set<ValueType> disabledTypes = EnumSet.noneOf(ValueType.class);

    private boolean outDirReady = false;

    private static final Map<ValueType, String> FILE_NAMES = Map.of(
            ValueType.INTEGER, "integers.txt",
            ValueType.FLOAT, "floats.txt",
            ValueType.STRING, "strings.txt"
    );

    public OutputManager(Path outDir, String prefix, boolean append) {
        this.outDir = outDir;
        this.prefix = prefix;
        this.append = append;
    }


    public boolean write(ValueType type, String line) {
        ensureOutDirReadyOrThrow();

        if (disabledTypes.contains(type)) {
            return false;
        }

        try {
            BufferedWriter writer = getWriter(type);

            if (type != ValueType.STRING) {
                line = line.strip();
            }

            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            disableType(type, e);
            return false;
        }
    }

    private void ensureOutDirReadyOrThrow() {
        if (outDirReady) {
            return;
        }

        try {
            Files.createDirectories(outDir);

            if (!Files.isDirectory(outDir)) {
                throw new IOException("Output path is not a directory: " + outDir);
            }
            if (!Files.isWritable(outDir)) {
                throw new AccessDeniedException(outDir.toString());
            }

            outDirReady = true;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot use output directory: " + outDir, e);
        }
    }

    private BufferedWriter getWriter(ValueType type) throws IOException {
        BufferedWriter existing = writers.get(type);
        if (existing != null) {
            return existing;
        }

        Path path = outputPath(type);

        BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING
        );

        writers.put(type, writer);
        return writer;
    }

    private Path outputPath(ValueType type) {
        String filename = prefix + FILE_NAMES.get(type);
        return outDir.resolve(filename);
    }

    private void disableType(ValueType type, IOException cause) {
        if (!disabledTypes.add(type)) {
            return;
        }

        BufferedWriter w = writers.remove(type);
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {}
        }

        String message = (cause.getMessage() != null) ? cause.getMessage() : cause.toString();
        System.err.println(
                "Cannot write values of type " + type + " to " + outputPath(type)
                        + ": " + message
                        + ". Further values of this type will be skipped."
        );
    }

    @Override
    public void close() throws IOException {
        IOException first = null;

        for (BufferedWriter writer : writers.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }
        }

        writers.clear();

        if (first != null) {
            throw first;
        }
    }
}