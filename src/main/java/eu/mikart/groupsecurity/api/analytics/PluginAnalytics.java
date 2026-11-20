package eu.mikart.groupsecurity.api.analytics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Entry point for in-process plugin analytics. Use the artifact as a provided dependency. */
public final class PluginAnalytics {
    private static final Pattern KEY = Pattern.compile("[a-zA-Z0-9_.:-]{1,64}");
    private static final AtomicReference<Registry> REGISTRY = new AtomicReference<>();

    private PluginAnalytics() {}

    /** Returns whether the host currently accepts analytics registrations. */
    public static boolean isAvailable() {
        return REGISTRY.get() != null;
    }

    /**
     * Registers a dashboard tab while the dashboard is running. Use a plugin-qualified ID.
     * Close the returned handle when your plugin shuts down.
     */
    public static Tab register(TabDefinition definition) {
        Objects.requireNonNull(definition);
        Registry registry = REGISTRY.get();
        if (registry == null) throw new IllegalStateException("Plugin analytics is unavailable");
        return registry.register(definition);
    }

    /** Runtime integration point for the host. */
    public static void attach(Registry registry) {
        if (!REGISTRY.compareAndSet(null, Objects.requireNonNull(registry))) {
            throw new IllegalStateException("Plugin analytics is already running");
        }
    }

    /** Runtime integration point for the host. */
    public static void detach(Registry registry) {
        REGISTRY.compareAndSet(Objects.requireNonNull(registry), null);
    }

    public interface Registry {
        Tab register(TabDefinition definition);
    }

    public interface Tab extends AutoCloseable {
        /**
         * Attempts to capture an event in the host's recent-event buffer. An accepted event
         * can be evicted as the buffer fills. Callers can retry a busy capture. */
        CaptureResult capture(Event event);

        @Override
        void close();
    }

    public enum CaptureResult { ACCEPTED, BUSY, CLOSED, UNDECLARED_EVENT }

    public enum Aggregation { COUNT, UNIQUE_ACTORS, SUM, AVERAGE }

    public record TabDefinition(String id, String title, List<Metric> metrics) {
        public TabDefinition {
            checkKey(id);
            checkText(title, 80);
            metrics = List.copyOf(metrics);
            if (metrics.isEmpty() || metrics.size() > 8) throw new IllegalArgumentException("Expected 1–8 metrics");
        }
    }

    /**
     * A dashboard metric for one event name. Filters use exact typed equality.
     * A breakdown may group by a string, boolean, or numeric property.
     */
    public record Metric(String label, String eventName, Aggregation aggregation, String numericProperty,
                         Map<String, Object> filters, String breakdownProperty) {
        public Metric {
            checkText(label, 80);
            checkKey(eventName);
            Objects.requireNonNull(aggregation);
            switch (aggregation) {
                case SUM, AVERAGE -> checkKey(numericProperty);
                case COUNT, UNIQUE_ACTORS -> {
                    if (numericProperty != null) throw new IllegalArgumentException("Unexpected numeric property");
                }
            }
            filters = copyProperties(filters, 8);
            if (breakdownProperty != null) checkKey(breakdownProperty);
        }
    }

    /** Use opaque actor identifiers and avoid collecting secrets or personal data in properties. */
    public record Event(String name, long timestampMillis, String actorId, Map<String, Object> properties) {
        public Event {
            checkKey(name);
            if (timestampMillis < 0) throw new IllegalArgumentException("Invalid timestamp");
            if (actorId != null) checkText(actorId, 128);
            properties = copyProperties(properties, 16);
        }
    }

    private static Map<String, Object> copyProperties(Map<String, Object> properties, int limit) {
        if (properties.size() > limit) throw new IllegalArgumentException("Too many properties");
        Map<String, Object> copy = Map.copyOf(properties);
        for (var entry : copy.entrySet()) {
            checkKey(entry.getKey());
            switch (entry.getValue()) {
                case String value -> checkText(value, 128);
                case Boolean ignored -> {}
                case Integer ignored -> {}
                case Long ignored -> {}
                case Double value when Double.isFinite(value) -> {}
                default -> throw new IllegalArgumentException("Expected a string, boolean, integer, long, or finite double");
            }
        }
        return copy;
    }

    private static void checkKey(String value) {
        if (!KEY.matcher(Objects.requireNonNull(value)).matches()) throw new IllegalArgumentException("Invalid analytics key");
    }

    private static void checkText(String value, int maxLength) {
        if (value.length() > maxLength || value.isBlank()) throw new IllegalArgumentException("Invalid analytics text");
    }
}
