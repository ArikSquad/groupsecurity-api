package eu.mikart.groupsecurity.api.analytics;

import java.math.BigDecimal;
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

    public record TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels,
                                List<Flow> flows, List<Heatmap> heatmaps, List<HistoricalFlow> historicalFlows) {
        public TabDefinition(String id, String title, List<Metric> metrics) {
            this(id, title, metrics, List.of());
        }

        public TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels) {
            this(id, title, metrics, funnels, List.of(), List.of());
        }

        public TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels,
                             List<Flow> flows, List<Heatmap> heatmaps) {
            this(id, title, metrics, funnels, flows, heatmaps, List.of());
        }

        public TabDefinition {
            checkKey(id);
            checkText(title, 80);
            metrics = List.copyOf(metrics);
            funnels = List.copyOf(funnels);
            flows = List.copyOf(flows);
            heatmaps = List.copyOf(heatmaps);
            historicalFlows = List.copyOf(historicalFlows);
            int widgets = metrics.size() + funnels.size() + flows.size() + heatmaps.size() + historicalFlows.size();
            if (widgets < 1 || widgets > 8) throw new IllegalArgumentException("Expected 1–8 widgets");
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

    /** Counts actors through ordered steps within a conversion window. */
    public record Funnel(String label, List<FunnelStep> steps, long conversionWindowMillis) {
        public Funnel {
            checkText(label, 80);
            steps = List.copyOf(steps);
            if (steps.size() < 2 || steps.size() > 8) throw new IllegalArgumentException("Expected 2–8 funnel steps");
            if (conversionWindowMillis <= 0) throw new IllegalArgumentException("Expected a positive conversion window");
        }
    }

    /** Matches an event and its exact typed property filters. */
    public record FunnelStep(String label, String eventName, Map<String, Object> filters) {
        public FunnelStep {
            checkText(label, 80);
            checkKey(eventName);
            filters = copyProperties(filters, 8);
        }
    }

    /**
     * Aggregates positive weights by directed source and destination. For currency, capture
     * integer minor units and use a filter to keep different currencies in separate widgets.
     */
    public record Flow(String label, String eventName, String sourceProperty, String destinationProperty,
                       String weightProperty, String unit, Map<String, Object> filters) {
        public Flow {
            checkText(label, 80);
            checkKey(eventName);
            checkKey(sourceProperty);
            checkKey(destinationProperty);
            checkKey(weightProperty);
            checkText(unit, 24);
            filters = copyProperties(filters, 8);
        }
    }

    /** A widget backed by the registering plugin's durable data source. */
    public record HistoricalFlow(String label, String unit, FlowQuery query) {
        public HistoricalFlow {
            checkText(label, 80);
            checkText(unit, 24);
            Objects.requireNonNull(query);
        }
    }

    @FunctionalInterface
    public interface FlowQuery {
        /**
         * Query the inclusive time range and return at most maxEdges of the largest routes.
         * This runs off the game thread. Apply a storage timeout and honor interruption.
         */
        FlowResult read(long fromMillis, long toMillis, int maxEdges) throws Exception;
    }

    public record FlowResult(BigDecimal total, long observations, long edgeCount, List<FlowEdge> edges) {
        public FlowResult {
            checkAmount(total);
            edges = List.copyOf(edges);
            if (observations < edgeCount || edgeCount < edges.size() || edges.size() > 50) {
                throw new IllegalArgumentException("Invalid flow result size");
            }
        }
    }

    public record FlowEdge(String source, String destination, BigDecimal amount) {
        public FlowEdge {
            checkText(source, 128);
            checkText(destination, 128);
            checkAmount(amount);
        }
    }

    private static void checkAmount(BigDecimal amount) {
        if (amount.signum() < 0 || amount.precision() > 64 || amount.scale() < -64 || amount.scale() > 64) {
            throw new IllegalArgumentException("Expected a non-negative, bounded decimal amount");
        }
    }

    public enum CoordinateSpace { GEOGRAPHIC, PLANAR }

    /**
     * Aggregates events into a fixed grid. Geographic coordinates use longitude as x and latitude
     * as y. A planar widget can use game coordinates, with positive y displayed upward.
     * A null weight property counts matching events.
     */
    public record Heatmap(String label, String eventName, CoordinateSpace space, String xProperty, String yProperty,
                          Bounds bounds, int resolution, String weightProperty, Map<String, Object> filters) {
        public Heatmap {
            checkText(label, 80);
            checkKey(eventName);
            Objects.requireNonNull(space);
            checkKey(xProperty);
            checkKey(yProperty);
            Objects.requireNonNull(bounds);
            if (resolution < 2 || resolution > 32) throw new IllegalArgumentException("Expected a grid size of 2–32");
            if (space == CoordinateSpace.GEOGRAPHIC &&
                (bounds.minX < -180 || bounds.maxX > 180 || bounds.minY < -85 || bounds.maxY > 85)) {
                throw new IllegalArgumentException("Geographic bounds exceed the supported Mercator range");
            }
            if (weightProperty != null) checkKey(weightProperty);
            filters = copyProperties(filters, 8);
        }
    }

    public record Bounds(double minX, double minY, double maxX, double maxY) {
        public Bounds {
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || maxX <= minX || maxY <= minY || !Double.isFinite(maxX - minX) || !Double.isFinite(maxY - minY)) {
                throw new IllegalArgumentException("Expected finite bounds with a positive span");
            }
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
