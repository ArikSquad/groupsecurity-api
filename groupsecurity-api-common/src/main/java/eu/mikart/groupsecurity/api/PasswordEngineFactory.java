package eu.mikart.groupsecurity.api;

/**
 * Creates the shared codec for a custom password engine.
 *
 * @see GroupSecurityAPI#registerEngine(PasswordEngineFactory)
 */
@FunctionalInterface
public interface PasswordEngineFactory {
    /**
     * Creates the codec to use for the lifetime of the engine registration.
     *
     * @return a non-null, thread-safe password codec
     */
    PasswordCodec create();
}
