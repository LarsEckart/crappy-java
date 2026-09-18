package crappyjava.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Folds javac's synthetic lambda methods ({@code lambda$foo$0}) into their enclosing method
 * by summing counters. {@code lambda$new$N} belongs to a constructor, {@code lambda$static$N}
 * to the static initializer. When the enclosing method name is ambiguous (overloads) or absent,
 * the lambda is kept as its own entry.
 */
public final class LambdaFolder {

    private static final Pattern LAMBDA = Pattern.compile("^lambda\\$(.+)\\$\\d+$");

    private LambdaFolder() {
    }

    public static List<MethodCoverage> fold(List<MethodCoverage> methods) {
        Map<String, List<MethodCoverage>> byClass = new LinkedHashMap<>();
        for (MethodCoverage method : methods) {
            byClass.computeIfAbsent(method.className(), k -> new ArrayList<>()).add(method);
        }
        List<MethodCoverage> result = new ArrayList<>(methods.size());
        for (List<MethodCoverage> classMethods : byClass.values()) {
            result.addAll(foldClass(classMethods));
        }
        return result;
    }

    private static List<MethodCoverage> foldClass(List<MethodCoverage> classMethods) {
        List<MethodCoverage> regular = new ArrayList<>();
        List<MethodCoverage> lambdas = new ArrayList<>();
        for (MethodCoverage method : classMethods) {
            (method.isLambda() ? lambdas : regular).add(method);
        }
        List<MethodCoverage> unfolded = new ArrayList<>();
        for (MethodCoverage lambda : lambdas) {
            int target = targetIndex(regular, lambda);
            if (target < 0) {
                unfolded.add(lambda);
            } else {
                regular.set(target, regular.get(target).plus(lambda));
            }
        }
        regular.addAll(unfolded);
        return regular;
    }

    /** Index of the unique enclosing method in {@code regular}, or -1. */
    static int targetIndex(List<MethodCoverage> regular, MethodCoverage lambda) {
        String enclosing = enclosingMethodName(lambda.name());
        if (enclosing == null) {
            return -1;
        }
        int found = -1;
        for (int i = 0; i < regular.size(); i++) {
            if (regular.get(i).name().equals(enclosing)) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    /** {@code lambda$foo$3} to {@code foo}; {@code new} to {@code <init>}; {@code static} to {@code <clinit>}. */
    static String enclosingMethodName(String lambdaName) {
        Matcher matcher = LAMBDA.matcher(lambdaName);
        if (!matcher.matches()) {
            return null;
        }
        String enclosing = matcher.group(1);
        return switch (enclosing) {
            case "new" -> "<init>";
            case "static" -> "<clinit>";
            default -> enclosing;
        };
    }
}
