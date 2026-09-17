package sample;

import java.util.List;

/** Two overloads of the same name, each with a lambda. Lambda folding is ambiguous here. */
public class Overloads {

    public List<Integer> map(List<Integer> xs) {
        return xs.stream().map(x -> x > 0 ? x : -x).toList();
    }

    public List<String> map(List<String> xs, String prefix) {
        return xs.stream().map(x -> x.isEmpty() ? prefix : prefix + x).toList();
    }
}
