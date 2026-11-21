package eu.mikart.groupsecurity.api;

import eu.mikart.groupsecurity.antibot.api.RiskSignalProvider;
import eu.mikart.groupsecurity.antibot.api.TrustModifierProvider;
import eu.mikart.groupsecurity.api.analytics.PluginAnalytics;
import eu.mikart.groupsecurity.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for integrations with a running GroupSecurity plugin.
 * <p>Declare GroupSecurity as a plugin dependency and obtain this API during
 * your plugin’s enable phase. Use {@link #isLoaded()} to check whether the
 * host has finished loading data before issuing user lookups.
 * <p>Lookups return futures. Schedule code that accesses game-server state
 * on the thread required by your platform.
 *
 * @see #getInstance()
 * @see #findInstance()
 * @see PluginAnalytics
 * @since 1.0
 */
@SuppressWarnings("unused")
public class GroupSecurityAPI {

	/**
	 * The instance published by the active platform binding.
	 */
	@ApiStatus.Internal
	protected static volatile GroupSecurityAPI instance;
	private final Backend backend;

	/**
	 * Creates a facade for a platform runtime.
	 *
	 * @param backend the runtime adapter
	 */
	@ApiStatus.Internal
	protected GroupSecurityAPI(Backend backend) {
		this.backend = Objects.requireNonNull(backend);
	}

	/**
	 * Operations implemented by the platform runtime for this facade.
	 */
	@ApiStatus.Internal
	public interface Backend {
		/**
		 * Reports whether the host has finished loading data.
		 *
		 * @return {@code true} once data is ready
		 */
		boolean isLoaded();

		/**
		 * Looks up a user by identifier.
		 *
		 * @param uuid the player identifier
		 * @return the pending lookup result
		 */
		CompletableFuture<Optional<User>> getUser(UUID uuid);

		/**
		 * Looks up a user by username.
		 *
		 * @param username the stored username to look up
		 * @return the pending lookup result
		 */
		CompletableFuture<Optional<User>> getUser(String username);

		/**
		 * Registers a custom password engine.
		 *
		 * @param factory the codec factory
		 */
		void registerEngine(PasswordEngineFactory factory);

		/**
		 * Adds a connection risk provider.
		 *
		 * @param provider the provider to add
		 */
		void registerRiskSignalProvider(RiskSignalProvider provider);

		/**
		 * Removes a connection risk provider.
		 *
		 * @param provider the provider to remove
		 */
		void unregisterRiskSignalProvider(RiskSignalProvider provider);

		/**
		 * Adds a trust provider.
		 *
		 * @param provider the provider to add
		 */
		void registerTrustModifierProvider(TrustModifierProvider provider);

		/**
		 * Removes a trust provider.
		 *
		 * @param provider the provider to remove
		 */
		void unregisterTrustModifierProvider(TrustModifierProvider provider);
	}

	/**
	 * Publishes a tab on the running analytics dashboard.
	 *
	 * @param definition the validated tab definition
	 * @return the registration and event capture handle
	 * @throws IllegalStateException if the dashboard is unavailable or its tab limit has been reached
	 * @throws IllegalArgumentException if a tab with this ID already exists
	 * @see PluginAnalytics#register(PluginAnalytics.TabDefinition)
	 */
	@NotNull
	public PluginAnalytics.Tab registerAnalyticsTab(@NotNull PluginAnalytics.TabDefinition definition) {
		return PluginAnalytics.register(definition);
	}

	/**
	 * Returns the API published by the active platform binding.
	 *
	 * @return the registered API instance
	 * @throws NotRegisteredException if no API instance is registered
	 * @see #findInstance()
	 * @since 1.0
	 */
	@NotNull
	public static GroupSecurityAPI getInstance() throws NotRegisteredException {
		GroupSecurityAPI current = instance;
		if (current == null) {
			throw new NotRegisteredException();
		}
		return current;
	}

	/**
	 * Looks up the active API for an optional integration.
	 *
	 * @return the published API, or an empty optional if none is registered
	 * @apiNote Registration and data readiness are separate lifecycle stages.
	 * Use {@link #isLoaded()} to check whether data is ready.
	 */
	@NotNull
	public static Optional<GroupSecurityAPI> findInstance() {
		return Optional.ofNullable(instance);
	}

	/**
	 * Clears the published API during host shutdown.
	 *
	 * @since 1.0
	 */
	@ApiStatus.Internal
	public static void unregister() {
		instance = null;
	}

	/**
	 * Reports whether the host has finished loading its data.
	 *
	 * @return {@code true} if host data is ready
	 * @since 1.0
	 */
	public boolean isLoaded() {
		return backend.isLoaded();
	}

	/**
	 * Looks up a stored user by player identifier.
	 *
	 * @param uuid the player identifier
	 * @return a future containing the user, or an empty optional when no record is returned
	 * @apiNote Future continuations may run on the thread that completes the lookup.
	 * @since 1.0
	 */
	@NotNull
	public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
		return backend.getUser(uuid);
	}

	/**
	 * Looks up a stored user by username.
	 *
	 * @param username the username to look up
	 * @return a future containing the user, or an empty optional when no record is returned
	 * @see #getUser(UUID)
	 * @since 1.0
	 */
	@NotNull
	public CompletableFuture<Optional<User>> getUser(@NotNull String username) {
		return backend.getUser(username);
	}

	/**
	 * Looks up the stored username for a player identifier.
	 *
	 * @param uuid the player identifier
	 * @return a future containing the username, or an empty optional when unavailable
	 * @see #getUser(UUID)
	 * @since 1.0
	 */
	@NotNull
	public CompletableFuture<Optional<String>> getUsername(@NotNull UUID uuid) {
		return getUser(uuid).thenApply(user -> user.map(User::getName));
	}

	/**
	 * Looks up the player identifier associated with a username.
	 *
	 * @param username the username to look up
	 * @return a future containing the player identifier, or an empty optional when unavailable
	 * @see #getUser(String)
	 * @since 1.0
	 */
	@NotNull
	public CompletableFuture<Optional<UUID>> getUserUuid(@NotNull String username) {
		return getUser(username).thenApply(user -> user.map(User::getUuid));
	}

	/**
	 * Registers a custom authentication engine backed by a shared codec.
	 *
	 * @param factory the factory invoked to create the engine’s codec
	 * @throws IllegalArgumentException if the codec identifier is already registered
	 * @throws NullPointerException if the factory or its result is {@code null}
	 * @see PasswordCodec
	 * @since 1.0
	 */
	public void registerEngine(@NotNull PasswordEngineFactory factory) {
		backend.registerEngine(factory);
	}

	/**
	 * Registers a callback that supplies connection risk signals.
	 *
	 * @param provider the provider to register
	 * @throws NullPointerException if the provider is {@code null}
	 * @see #unregisterRiskSignalProvider(RiskSignalProvider)
	 */
	public void registerRiskSignalProvider(@NotNull RiskSignalProvider provider) {
		backend.registerRiskSignalProvider(Objects.requireNonNull(provider, "provider"));
	}

	/**
	 * Removes a connection risk signal provider. An evaluation already in progress may
	 * still invoke the provider.
	 *
	 * @param provider the previously registered provider
	 */
	public void unregisterRiskSignalProvider(@NotNull RiskSignalProvider provider) {
		backend.unregisterRiskSignalProvider(provider);
	}

	/**
	 * Registers a callback that supplies trust modifiers.
	 *
	 * @param provider the provider to register
	 * @throws NullPointerException if the provider is {@code null}
	 * @see #unregisterTrustModifierProvider(TrustModifierProvider)
	 */
	public void registerTrustModifierProvider(@NotNull TrustModifierProvider provider) {
		backend.registerTrustModifierProvider(Objects.requireNonNull(provider, "provider"));
	}

	/**
	 * Removes a trust modifier provider. An evaluation already in progress may
	 * still invoke the provider.
	 *
	 * @param provider the previously registered provider
	 */
	public void unregisterTrustModifierProvider(@NotNull TrustModifierProvider provider) {
		backend.unregisterTrustModifierProvider(provider);
	}

	/**
	 * Indicates that no API is registered for the requested platform binding.
	 *
	 * @see #getInstance()
	 * @since 1.0
	 */
	public static final class NotRegisteredException extends IllegalStateException {

		private static final String MESSAGE = """
				Could not access the GroupSecurity API as it has not yet been registered. This could be because:
				1) GroupSecurity has failed to enable successfully
				2) Your plugin isn't set to load after GroupSecurity has
				   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
				3) You are attempting to access GroupSecurity on plugin construction/before your plugin has enabled.""";

		NotRegisteredException() {
			super(MESSAGE);
		}

	}
}
