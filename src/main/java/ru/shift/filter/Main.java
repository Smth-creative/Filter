package ru.shift.filter;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new FilterApp())
                .setParameterExceptionHandler(ExceptionHandler::onParseError)
                .execute(args);

        System.exit(exitCode);
    }
}
