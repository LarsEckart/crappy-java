package crap4java.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parsed command line. */
record CliArguments(boolean help, double threshold, boolean showAll, int top, List<Path> reports) {

    static final double DEFAULT_THRESHOLD = 30.0;

    static CliArguments parse(String[] args) {
        boolean help = false;
        double threshold = DEFAULT_THRESHOLD;
        boolean showAll = false;
        int top = 0;
        List<Path> reports = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                return new CliArguments(true, threshold, showAll, top, List.of());
            }
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--all" -> showAll = true;
                case "--threshold" -> threshold = parseDouble(arg, valueOf(args, ++i, arg));
                case "--top" -> top = parseTop(valueOf(args, ++i, arg));
                default -> {
                    if (arg.startsWith("-")) {
                        throw new UsageException("unknown option: " + arg);
                    }
                    reports.add(Path.of(arg));
                }
            }
        }
        if (showAll && top > 0) {
            throw new UsageException("--all and --top are mutually exclusive");
        }
        if (reports.isEmpty()) {
            throw new UsageException("no JaCoCo XML report given");
        }
        return new CliArguments(help, threshold, showAll, top, List.copyOf(reports));
    }

    private static String valueOf(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException(option + " requires a value");
        }
        return args[index];
    }

    private static double parseDouble(String option, String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0 || !Double.isFinite(parsed)) {
                throw new UsageException(option + " must be a non-negative number, got " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new UsageException(option + " must be a number, got " + value);
        }
    }

    private static int parseTop(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new UsageException("--top must be a positive integer, got " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new UsageException("--top must be a positive integer, got " + value);
        }
    }
}
