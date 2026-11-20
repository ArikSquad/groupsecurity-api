package eu.mikart.groupsecurity.api;

import eu.mikart.groupsecurity.antibot.api.RiskSignalProvider;
import eu.mikart.groupsecurity.antibot.api.TrustModifierProvider;
import eu.mikart.groupsecurity.user.User;
import eu.mikart.groupsecurity.api.analytics.PluginAnalytics;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The GroupSecurity API
 * <p>
 * Get the singleton instance with {@link #getInstance()}.
 *
 * @since 1.0
 */
@SuppressWarnings("unused")
public class GroupSecurityAPI {

	protected static volatile GroupSecurityAPI instance;
	private final Backend backend;

	protected GroupSecurityAPI(Backend backend) {
		this.backend = Objects.requireNonNull(backend);
	}

	@ApiStatus.Internal
	public interface Backend {
		boolean isLoaded();
		CompletableFuture<Optional<User>> getUser(UUID uuid);
		CompletableFuture<Optional<User>> getUser(String username);
		void registerEngine(PasswordEngineFactory factory);
		void registerRiskSignalProvider(RiskSignalProvider provider);
		void unregisterRiskSignalProvider(RiskSignalProvider provider);
		void registerTrustModifierProvider(TrustModifierProvider provider);
		void unregisterTrustModifierProvider(TrustModifierProvider provider);
	}

	public PluginAnalytics.Tab registerAnalyticsTab(PluginAnalytics.TabDefinition definition) {
		return PluginAnalytics.register(definition);
	}

	/**
	 * Get an instance of the GroupSecurity API.
	 *
	 * @return instance of the GroupSecurity API
	 * @throws NotRegisteredException if the API has not yet been registered.
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
	 * <b>(Internal use only)</b> - Unregister the API instance.
	 *
	 * @since 1.0
	 */
	@ApiStatus.Internal
	public static void unregister() {
		instance = null;
	}

	/**
	 * Returns if the plugin has finished loading data
	 *
	 * @return {@code true} if the plugin has finished loading data, {@code false} otherwise
	 * @since 1.0
	 */
	public boolean isLoaded() {
		return backend.isLoaded();
	}

	/**
	 * Get a {@link User} by their {@link UUID}
	 *
	 * @param user the {@link UUID} of the user
	 * @return An optional {@link User}, if they exist, in a future completing when the database lookup has completed
	 * @since 1.0
	 */
	public CompletableFuture<Optional<User>> getUser(@NotNull UUID user) {
		return backend.getUser(user);
	}

	/**
	 * Get a {@link User} by their username
	 *
	 * @param username The username of the user to get
	 * @return A future containing the user, or an empty optional if the user doesn't exist
	 * @since 1.0
	 */
	@NotNull
	public CompletableFuture<Optional<User>> getUser(@NotNull String username) {
		return backend.getUser(username);
	}

	/**
	 * Get a player's Minecraft username by their {@link UUID}
	 *
	 * @param uuid the {@link UUID} of the player
	 * @return the player's Minecraft username, if they exist, in a future completing when the database lookup has completed
	 * @since 1.0
	 */
	public CompletableFuture<Optional<String>> getUsername(@NotNull UUID uuid) {
		return getUser(uuid).thenApply(user -> user.map(User::getName));
	}


	/**
	 * Get a player's {@link UUID} by their Minecraft username
	 *
	 * @param username the Minecraft username of the player
	 * @return the player's {@link UUID}, if they exist, in a future completing when the database lookup has completed
	 * @since 1.0
	 */
	public CompletableFuture<Optional<UUID>> getUserUuid(@NotNull String username) {
		return getUser(username).thenApply(user -> user.map(User::getUuid));
	}

	/**
	 * Register a password codec using a factory.
	 *
	 * @param factory A factory that creates the password engine
	 * @throws IllegalArgumentException if an engine with the given ID is already registered
	 * @since 1.0
	 */
	public void registerEngine(@NotNull PasswordEngineFactory factory) {
		backend.registerEngine(factory);
	}

	public void registerRiskSignalProvider(@NotNull RiskSignalProvider provider) {
		backend.registerRiskSignalProvider(provider);
	}

	public void unregisterRiskSignalProvider(@NotNull RiskSignalProvider provider) {
		backend.unregisterRiskSignalProvider(provider);
	}

	public void registerTrustModifierProvider(@NotNull TrustModifierProvider provider) {
		backend.registerTrustModifierProvider(provider);
	}

	public void unregisterTrustModifierProvider(@NotNull TrustModifierProvider provider) {
		backend.unregisterTrustModifierProvider(provider);
	}

	/**
	 * An exception indicating the plugin has been accessed before it has been registered.
	 *
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
