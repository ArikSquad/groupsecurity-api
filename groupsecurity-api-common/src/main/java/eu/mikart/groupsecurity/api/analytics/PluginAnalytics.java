package eu.mikart.groupsecurity.api.analytics;

import org.jetbrains.annotations.ApiStatus;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers dashboards and captures in-process analytics for another plugin.
 * <p>Declare the API as a provided or compile-only dependency. Register a
 * {@linkplain TabDefinition tab definition} while the dashboard is available,
 * retain the returned {@link Tab}, and close it when your plugin shuts down.
 * <p>Keys contain 1 to 64 ASCII letters, digits, underscores, periods, colons,
 * or hyphens. Property values support {@link String}, {@link Boolean},
 * {@link Integer}, {@link Long}, and finite {@link Double} values. Property
 * filters use typed equality, so an integer and a long with the same value
 * are distinct.
 *
 * <p>For example, a plugin can register a counter and retain the returned
 * handle for the lifetime of the integration.
 * {@snippet :
 * var definition = PluginAnalytics.TabDefinition.builder("example:activity", "Activity")
 *     .addMetric(new PluginAnalytics.Metric("Rounds completed", "round.completed",
 *         PluginAnalytics.Aggregation.COUNT, null, Map.of(), null))
 *     .build();
 *
 * var tab = PluginAnalytics.register(definition);
 * var result = tab.capture(new PluginAnalytics.Event(
 *     "round.completed", System.currentTimeMillis(), null, Map.of()));
 * // Retain tab and call tab.close() during plugin shutdown.
 * }
 *
 * @see TabDefinition#builder(String, String)
 */
public final class PluginAnalytics {
    private static final AtomicReference<Registry> REGISTRY = new AtomicReference<>();

    private PluginAnalytics() {}

    /**
     * Reports whether a dashboard registry is currently attached.
     *
     * @return {@code true} while a dashboard is available for registration
     * @apiNote The dashboard may stop before a subsequent call to {@link #register}.
     */
    public static boolean isAvailable() {
        return REGISTRY.get() != null;
    }

    /**
     * Registers a dashboard tab with the running host.
     *
     * @param definition the tab to publish, with a plugin-qualified ID
     * @return the handle used to capture events and remove the tab
     * @throws NullPointerException if {@code definition} is {@code null}
     * @throws IllegalStateException if the dashboard is unavailable or its tab limit has been reached
     * @throws IllegalArgumentException if a tab with this ID already exists
     */
    public static Tab register(TabDefinition definition) {
        Objects.requireNonNull(definition);
        Registry registry = REGISTRY.get();
        if (registry == null) throw new IllegalStateException("Plugin analytics is unavailable");
        return registry.register(definition);
    }

    /**
     * Publishes the dashboard registry for in-process integrations.
     *
     * @param registry the active host registry
     * @throws IllegalStateException if a registry is already attached
     */
    @ApiStatus.Internal
    public static void attach(Registry registry) {
        if (!REGISTRY.compareAndSet(null, Objects.requireNonNull(registry))) {
            throw new IllegalStateException("Plugin analytics is already running");
        }
    }

    /**
     * Clears the registry if it still belongs to the supplied host.
     *
     * @param registry the host registry being stopped
     */
    @ApiStatus.Internal
    public static void detach(Registry registry) {
        REGISTRY.compareAndSet(Objects.requireNonNull(registry), null);
    }

    /**
     * Host adapter for publishing analytics tabs.
     */
    @ApiStatus.Internal
    public interface Registry {
        /**
         * Creates a tab owned by the active dashboard.
         *
         * @param definition the validated tab definition
         * @return the registered tab handle
         */
        Tab register(TabDefinition definition);
    }

    /**
     * A live dashboard registration and the event capture handle for that tab.
     */
    public interface Tab extends AutoCloseable {
        /**
         * Attempts to append an event to the host’s bounded recent-event buffer.
         *
         * @param event the event to capture
         * @return the outcome of the capture attempt
         * @apiNote Accepted events may be evicted as the buffer fills. A caller may
         * retry a {@link CaptureResult#BUSY} capture.
         */
        CaptureResult capture(Event event);

        /**
         * Reports whether this registration is still active.
         *
         * @return {@code true} if the handle still belongs to the running dashboard
         * @implSpec The default implementation returns {@code true}.
         */
        default boolean isOpen() {
            return true;
        }

        /**
         * Removes the tab and releases its event buffer. Closing an already closed
         * registration has no effect.
         */
        @Override
        void close();
    }

    /**
     * Outcome of one event capture attempt.
     */
    public enum CaptureResult {
        /** The event was appended to the buffer. */
        ACCEPTED,
        /** The buffer could not accept the event during this attempt. */
        BUSY,
        /** The registration is no longer active. */
        CLOSED,
        /** No event-backed widget declares this event name. */
        UNDECLARED_EVENT
    }

    /**
     * Reduction applied to the events that match a metric.
     */
    public enum Aggregation {
        /** Counts matching events. */
        COUNT,
        /** Counts distinct non-null actor identifiers. */
        UNIQUE_ACTORS,
        /** Adds the numeric property values. */
        SUM,
        /** Computes the arithmetic mean of the numeric property values. */
        AVERAGE
    }

    /**
     * An immutable dashboard tab containing between one and eight widgets.
     *
     * @param id stable, plugin-qualified tab key
     * @param title display title, up to 80 characters
     * @param metrics event metrics
     * @param funnels ordered conversion funnels
     * @param flows event-backed flows
     * @param heatmaps event-backed heatmaps
     * @param historicalFlows flows backed by a plugin data source
     * @param tables tables backed by a plugin data source
     */
    public record TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels,
                                List<Flow> flows, List<Heatmap> heatmaps, List<HistoricalFlow> historicalFlows,
                                List<HistoricalTable> tables) {
        /**
         * Creates a tab with event metrics.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @param metrics event metrics
         * @throws IllegalArgumentException if the ID, title, or widget count is invalid
         */
        public TabDefinition(String id, String title, List<Metric> metrics) {
            this(id, title, metrics, List.of());
        }

        /**
         * Creates a tab with metrics and funnels.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @param metrics event metrics
         * @param funnels ordered conversion funnels
         * @throws IllegalArgumentException if the ID, title, or widget count is invalid
         */
        public TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels) {
            this(id, title, metrics, funnels, List.of(), List.of());
        }

        /**
         * Creates a tab with event-backed widgets.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @param metrics event metrics
         * @param funnels ordered conversion funnels
         * @param flows event-backed flows
         * @param heatmaps event-backed heatmaps
         * @throws IllegalArgumentException if the ID, title, or widget count is invalid
         */
        public TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels,
                             List<Flow> flows, List<Heatmap> heatmaps) {
            this(id, title, metrics, funnels, flows, heatmaps, List.of());
        }

        /**
         * Creates a tab with event-backed widgets and historical flows.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @param metrics event metrics
         * @param funnels ordered conversion funnels
         * @param flows event-backed flows
         * @param heatmaps event-backed heatmaps
         * @param historicalFlows flows backed by a plugin data source
         * @throws IllegalArgumentException if the ID, title, or widget count is invalid
         */
        public TabDefinition(String id, String title, List<Metric> metrics, List<Funnel> funnels,
                             List<Flow> flows, List<Heatmap> heatmaps, List<HistoricalFlow> historicalFlows) {
            this(id, title, metrics, funnels, flows, heatmaps, historicalFlows, List.of());
        }

        /**
         * Creates a tab and takes an immutable snapshot of each widget list.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @param metrics event metrics
         * @param funnels ordered conversion funnels
         * @param flows event-backed flows
         * @param heatmaps event-backed heatmaps
         * @param historicalFlows flows backed by a plugin data source
         * @param tables tables backed by a plugin data source
         * @throws IllegalArgumentException if the ID, title, or widget count is invalid
         */
        public TabDefinition {
            checkKey(id);
            checkText(title, 80);
            metrics = List.copyOf(metrics);
            funnels = List.copyOf(funnels);
            flows = List.copyOf(flows);
            heatmaps = List.copyOf(heatmaps);
            historicalFlows = List.copyOf(historicalFlows);
            tables = List.copyOf(tables);
            int widgets = metrics.size() + funnels.size() + flows.size() + heatmaps.size() + historicalFlows.size() + tables.size();
            if (widgets < 1 || widgets > 8) throw new IllegalArgumentException("Expected 1–8 widgets");
        }

        /**
         * Starts a dashboard definition with no widgets.
         *
         * @param id stable, plugin-qualified tab key
         * @param title display title
         * @return a builder to populate before registration
         */
        public static Builder builder(String id, String title) {
            return new Builder(id, title);
        }

        /**
         * Builds a tab by adding individual widgets. Each call to {@link #build()}
         * creates an immutable snapshot. A builder is intended for use by one thread.
         */
        public static final class Builder {
            private final String id;
            private final String title;
            private final List<Metric> metrics = new ArrayList<>();
            private final List<Funnel> funnels = new ArrayList<>();
            private final List<Flow> flows = new ArrayList<>();
            private final List<Heatmap> heatmaps = new ArrayList<>();
            private final List<HistoricalFlow> historicalFlows = new ArrayList<>();
            private final List<HistoricalTable> tables = new ArrayList<>();

            private Builder(String id, String title) {
                this.id = id;
                this.title = title;
            }

            /**
             * Adds a metric to the definition.
             *
             * @param widget the metric to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addMetric(Metric widget) {
                metrics.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Adds a funnel to the definition.
             *
             * @param widget the funnel to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addFunnel(Funnel widget) {
                funnels.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Adds a flow to the definition.
             *
             * @param widget the flow to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addFlow(Flow widget) {
                flows.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Adds a heatmap to the definition.
             *
             * @param widget the heatmap to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addHeatmap(Heatmap widget) {
                heatmaps.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Adds a historical flow to the definition.
             *
             * @param widget the historical flow to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addHistoricalFlow(HistoricalFlow widget) {
                historicalFlows.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Adds a historical table to the definition.
             *
             * @param widget the historical table to add
             * @return this builder
             * @throws NullPointerException if the widget is {@code null}
             */
            public Builder addHistoricalTable(HistoricalTable widget) {
                tables.add(Objects.requireNonNull(widget, "widget"));
                return this;
            }

            /**
             * Creates the immutable dashboard definition.
             *
             * @return a snapshot of the current widgets
             * @throws IllegalArgumentException if the ID, title, or widget count is invalid
             */
            public TabDefinition build() {
                return new TabDefinition(id, title, metrics, funnels, flows, heatmaps, historicalFlows, tables);
            }
        }
    }

    /**
     * A reduction over matching events, optionally grouped by a property.
     *
     * @param label display label
     * @param eventName event key to match
     * @param aggregation operation applied to matching events
     * @param numericProperty numeric property for sum and average, or {@code null} for other operations
     * @param filters up to eight exact, typed property filters
     * @param breakdownProperty property used for grouping, or {@code null} for no breakdown
     */
    public record Metric(String label, String eventName, Aggregation aggregation, String numericProperty,
                         Map<String, Object> filters, String breakdownProperty) {
        /**
         * Creates a validated metric.
         *
         * @param label display label
         * @param eventName event key to match
         * @param aggregation operation applied to matching events
         * @param numericProperty numeric property for sum and average, or {@code null} for other operations
         * @param filters up to eight exact, typed property filters
         * @param breakdownProperty property used for grouping, or {@code null} for no breakdown
         * @throws IllegalArgumentException if the label, event key, numeric property, breakdown key, or filters are invalid
         */
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

    /**
     * Tracks actors through ordered steps within a conversion window.
     *
     * @param label display label
     * @param steps ordered list of two to eight steps
     * @param conversionWindowMillis positive maximum interval from the first step to the last
     */
    public record Funnel(String label, List<FunnelStep> steps, long conversionWindowMillis) {
        /**
         * Creates a validated funnel.
         *
         * @param label display label
         * @param steps ordered list of two to eight steps
         * @param conversionWindowMillis positive maximum interval from the first step to the last
         * @throws IllegalArgumentException if the label, step count, or conversion window is invalid
         */
        public Funnel {
            checkText(label, 80);
            steps = List.copyOf(steps);
            if (steps.size() < 2 || steps.size() > 8) throw new IllegalArgumentException("Expected 2–8 funnel steps");
            if (conversionWindowMillis <= 0) throw new IllegalArgumentException("Expected a positive conversion window");
        }
    }

    /**
     * An event match within a conversion funnel.
     *
     * @param label display label
     * @param eventName event key to match
     * @param filters up to eight exact, typed property filters
     */
    public record FunnelStep(String label, String eventName, Map<String, Object> filters) {
        /**
         * Creates a validated funnel step.
         *
         * @param label display label
         * @param eventName event key to match
         * @param filters up to eight exact, typed property filters
         * @throws IllegalArgumentException if the label, event key, or filters are invalid
         */
        public FunnelStep {
            checkText(label, 80);
            checkKey(eventName);
            filters = copyProperties(filters, 8);
        }
    }

    /**
     * Aggregates positive weights by directed source and destination.
     * <p>For currency, capture integer minor units and filter by currency.
     *
     * @param label display label
     * @param eventName event key to match
     * @param sourceProperty property identifying the origin
     * @param destinationProperty property identifying the destination
     * @param weightProperty numeric property to sum for each route
     * @param unit display unit, up to 24 characters
     * @param filters up to eight exact, typed property filters
     */
    public record Flow(String label, String eventName, String sourceProperty, String destinationProperty,
                       String weightProperty, String unit, Map<String, Object> filters) {
        /**
         * Creates a validated flow.
         *
         * @param label display label
         * @param eventName event key to match
         * @param sourceProperty property identifying the origin
         * @param destinationProperty property identifying the destination
         * @param weightProperty numeric property to sum for each route
         * @param unit display unit, up to 24 characters
         * @param filters up to eight exact, typed property filters
         * @throws IllegalArgumentException if a key, display label, unit, or filter is invalid
         */
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

    /**
     * A flow widget backed by the registering plugin’s durable data source.
     *
     * @param label display label
     * @param unit display unit, up to 24 characters
     * @param query callback used to read historical data
     */
    public record HistoricalFlow(String label, String unit, FlowQuery query) {
        /**
         * Creates a validated historical flow.
         *
         * @param label display label
         * @param unit display unit, up to 24 characters
         * @param query callback used to read historical data
         * @throws IllegalArgumentException if the display label or unit is invalid
         */
        public HistoricalFlow {
            checkText(label, 80);
            checkText(unit, 24);
            Objects.requireNonNull(query);
        }
    }

    /**
     * Reads historical flow data for a requested time range.
     */
    @FunctionalInterface
    public interface FlowQuery {
        /**
         * Reads totals for the range and returns the largest routes.
         *
         * @param fromMillis inclusive start in epoch milliseconds
         * @param toMillis inclusive end in epoch milliseconds
         * @param maxEdges maximum number of routes to return
         * @return totals for the entire range and up to {@code maxEdges} routes
         * @throws Exception if the query fails or is interrupted
         * @implSpec The host invokes this method off the game thread. Bound storage
         * execution time, honor interruption, and allow for concurrent queries.
         */
        FlowResult read(long fromMillis, long toMillis, int maxEdges) throws Exception;
    }

    /**
     * Historical totals and the returned subset of routes.
     *
     * @param total non-negative weight summed across all routes in the range
     * @param observations number of observations in the range
     * @param edgeCount number of distinct routes in the range
     * @param edges returned routes, up to 50
     */
    public record FlowResult(BigDecimal total, long observations, long edgeCount, List<FlowEdge> edges) {
        /**
         * Creates a validated flow result.
         *
         * @param total non-negative weight summed across all routes in the range
         * @param observations number of observations in the range
         * @param edgeCount number of distinct routes in the range
         * @param edges returned routes, up to 50
         * @throws IllegalArgumentException if the counts or total are invalid, or more than 50 routes are supplied
         */
        public FlowResult {
            checkAmount(total);
            edges = List.copyOf(edges);
            if (observations < edgeCount || edgeCount < edges.size() || edges.size() > 50) {
                throw new IllegalArgumentException("Invalid flow result size");
            }
        }
    }

    /**
     * An aggregated route from one origin to one destination.
     *
     * @param source origin label
     * @param destination destination label
     * @param amount non-negative weight for this route
     */
    public record FlowEdge(String source, String destination, BigDecimal amount) {
        /**
         * Creates a validated flow edge.
         *
         * @param source origin label
         * @param destination destination label
         * @param amount non-negative weight for this route
         * @throws IllegalArgumentException if an endpoint label or the amount is invalid
         */
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

    /**
     * A paginated, read-only report over data owned by the registering plugin.
     *
     * @param label display label
     * @param columns ordered list of one to 32 columns
     * @param query callback used to read pages
     */
    public record HistoricalTable(String label, List<Column> columns, TableQuery query) {
        /**
         * Creates a validated historical table.
         *
         * @param label display label
         * @param columns ordered list of one to 32 columns
         * @param query callback used to read pages
         * @throws IllegalArgumentException if the display label or column count is invalid
         */
        public HistoricalTable {
            checkText(label, 80);
            columns = List.copyOf(columns);
            if (columns.isEmpty() || columns.size() > 32) throw new IllegalArgumentException("Invalid columns");
            Objects.requireNonNull(query);
        }
    }

    /**
     * A displayed column in a historical table.
     *
     * @param label column heading
     * @param filterKey drill-down filter key, or {@code null} to disable drill-down for this column
     */
    public record Column(String label, String filterKey) {
        /**
         * Creates a validated column.
         *
         * @param label column heading
         * @param filterKey drill-down filter key, or {@code null} to disable drill-down for this column
         * @throws IllegalArgumentException if the heading or filter key is invalid
         */
        public Column {
            checkText(label, 80);
            if (filterKey != null) checkKey(filterKey);
        }
    }

    /**
     * Reads a page of rows for a historical table.
     */
    @FunctionalInterface
    public interface TableQuery {
        /**
         * Reads the requested page, applying its time range and filters.
         *
         * @param request the range, filters, and pagination settings
         * @return up to the requested row limit, with one string cell per column
         * in definition order
         * @throws Exception if the query fails or is interrupted
         * @implSpec The host invokes this method off the game thread. Honor
         * interruption and use stable ordering so adjacent pages are consistent.
         */
        List<List<String>> read(TableRequest request) throws Exception;
    }

    /**
     * The bounded parameters for a historical table query.
     *
     * @param fromMillis inclusive start in epoch milliseconds
     * @param toMillis inclusive end in epoch milliseconds
     * @param filters drill-down values indexed by column filter key
     * @param offset number of matching rows to skip, from 0 to 100,000
     * @param limit maximum number of rows to return, from 1 to 100
     */
    public record TableRequest(long fromMillis, long toMillis, Map<String, String> filters, int offset, int limit) {
        /**
         * Creates a validated table request.
         *
         * @param fromMillis inclusive start in epoch milliseconds
         * @param toMillis inclusive end in epoch milliseconds
         * @param filters drill-down values indexed by column filter key
         * @param offset number of matching rows to skip, from 0 to 100,000
         * @param limit maximum number of rows to return, from 1 to 100
         * @throws IllegalArgumentException if the range, filters, or pagination settings are invalid
         */
        public TableRequest {
            if (fromMillis < 0 || toMillis < fromMillis || offset < 0 || offset > 100_000
                || limit < 1 || limit > 100 || filters.size() > 16) {
                throw new IllegalArgumentException("Invalid report request");
            }
            filters = Map.copyOf(filters);
            filters.forEach((key, value) -> {
                checkKey(key);
                checkText(value, 128);
            });
        }
    }

    /**
     * Coordinate systems supported by a heatmap.
     */
    public enum CoordinateSpace {
        /** Longitude and latitude in degrees. */
        GEOGRAPHIC,
        /** Cartesian coordinates, with positive y displayed upward. */
        PLANAR
    }

    /**
     * Aggregates matching events into a fixed square grid.
     *
     * @param label display label
     * @param eventName event key to match
     * @param space coordinate system for the grid
     * @param xProperty numeric horizontal coordinate, or longitude in geographic space
     * @param yProperty numeric vertical coordinate, or latitude in geographic space
     * @param bounds grid extent, within longitude ±180 and latitude ±85 in geographic space
     * @param resolution number of cells along each axis, from 2 to 32
     * @param weightProperty numeric weight property, or {@code null} to count events
     * @param filters up to eight exact, typed property filters
     */
    public record Heatmap(String label, String eventName, CoordinateSpace space, String xProperty, String yProperty,
                          Bounds bounds, int resolution, String weightProperty, Map<String, Object> filters) {
        /**
         * Creates a validated heatmap.
         *
         * @param label display label
         * @param eventName event key to match
         * @param space coordinate system for the grid
         * @param xProperty numeric horizontal coordinate, or longitude in geographic space
         * @param yProperty numeric vertical coordinate, or latitude in geographic space
         * @param bounds grid extent, within longitude ±180 and latitude ±85 in geographic space
         * @param resolution number of cells along each axis, from 2 to 32
         * @param weightProperty numeric weight property, or {@code null} to count events
         * @param filters up to eight exact, typed property filters
         * @throws IllegalArgumentException if a key, label, bound, resolution, or filter is invalid
         */
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

    /**
     * Finite coordinate bounds with a positive span along each axis.
     *
     * @param minX lower horizontal boundary
     * @param minY lower vertical boundary
     * @param maxX upper horizontal boundary
     * @param maxY upper vertical boundary
     */
    public record Bounds(double minX, double minY, double maxX, double maxY) {
        /**
         * Creates a validated coordinate extent.
         *
         * @param minX lower horizontal boundary
         * @param minY lower vertical boundary
         * @param maxX upper horizontal boundary
         * @param maxY upper vertical boundary
         * @throws IllegalArgumentException if a coordinate or span is non-finite, or a span is not positive
         */
        public Bounds {
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || maxX <= minX || maxY <= minY || !Double.isFinite(maxX - minX) || !Double.isFinite(maxY - minY)) {
                throw new IllegalArgumentException("Expected finite bounds with a positive span");
            }
        }
    }

    /**
     * An immutable observation captured by a dashboard tab.
     * <p>Use opaque actor identifiers and keep secrets and personal data out of
     * analytics properties.
     *
     * @param name declared event key
     * @param timestampMillis observation time in epoch milliseconds
     * @param actorId opaque identifier, or {@code null} for an anonymous observation
     * @param properties up to 16 supported scalar properties
     */
    public record Event(String name, long timestampMillis, String actorId, Map<String, Object> properties) {
        /**
         * Creates a validated event.
         *
         * @param name declared event key
         * @param timestampMillis observation time in epoch milliseconds
         * @param actorId opaque identifier, or {@code null} for an anonymous observation
         * @param properties up to 16 supported scalar properties
         * @throws IllegalArgumentException if the key, timestamp, actor identifier, or properties are invalid
         */
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
        int length = value.length();
        if (length == 0 || length > 64) throw new IllegalArgumentException("Invalid analytics key");
        for (int index = 0; index < length; index++) {
            char character = value.charAt(index);
            boolean letterOrDigit = (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9');
            if (!letterOrDigit && character != '_' && character != '.' && character != ':' && character != '-') {
                throw new IllegalArgumentException("Invalid analytics key");
            }
        }
    }

    private static void checkText(String value, int maxLength) {
        if (value.length() > maxLength || value.isBlank()) throw new IllegalArgumentException("Invalid analytics text");
    }
}
