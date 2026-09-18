package crappyjava.gradle;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/** {@code crappyJava { ... }} block. Values become the conventions of the {@code crap} task. */
public class CrappyJavaExtension {

    private final Property<Double> threshold;
    private final Property<Boolean> failOnViolation;
    private final Property<Boolean> showAll;
    private final Property<Integer> top;

    @Inject
    public CrappyJavaExtension(ObjectFactory objects) {
        threshold = objects.property(Double.class).convention(30.0);
        failOnViolation = objects.property(Boolean.class).convention(true);
        showAll = objects.property(Boolean.class).convention(false);
        top = objects.property(Integer.class).convention(0);
    }

    /** CRAP score above which a method is crappy. Default 30. */
    public Property<Double> getThreshold() {
        return threshold;
    }

    /** Accepts any number so Groovy DSL can write {@code threshold = 10} or {@code threshold = 7.5}. */
    public void setThreshold(Number value) {
        threshold.set(value.doubleValue());
    }

    /** Fail the build when any method is above the threshold. Default true. */
    public Property<Boolean> getFailOnViolation() {
        return failOnViolation;
    }

    /** List every method instead of only crappy ones. Default false. */
    public Property<Boolean> getShowAll() {
        return showAll;
    }

    /** List the n worst methods regardless of threshold; 0 disables. Default 0. */
    public Property<Integer> getTop() {
        return top;
    }
}
