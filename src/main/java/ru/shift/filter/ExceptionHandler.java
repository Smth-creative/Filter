package ru.shift.filter;

import picocli.CommandLine;

import java.io.PrintWriter;

public class ExceptionHandler {
    public static int onParseError(CommandLine.ParameterException e, String[] args) {
        CommandLine cmd = e.getCommandLine();
        PrintWriter err = cmd.getErr();

        if (e instanceof CommandLine.UnmatchedArgumentException) {
            err.println("Unknown option/parameter. Use option --help for additional information");
        } else if (e instanceof CommandLine.MissingParameterException) {
            err.println("Lack of required parameters: " + e.getMessage());
            err.println("Use option --help for additional information");
        } else if (e instanceof CommandLine.MutuallyExclusiveArgsException) {
            err.println("Options -s and -f are mutually exclusive (specify only one).");
            err.println("Use --help for additional information");
        } else {
            err.println("Unexpected message: " + e.getMessage());
        }

        return cmd.getCommandSpec().exitCodeOnInvalidInput();
    }
}
