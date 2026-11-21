package eu.mikart.groupsecurity.api;

/**
 * Encodes and verifies passwords for a GroupSecurity authentication engine.
 * <p>Implementations must be safe to invoke concurrently. GroupSecurity stores
 * the encoded value and supplies it back to the codec during verification.
 *
 * @see PasswordEngineFactory
 */
public interface PasswordCodec {
    /**
     * Returns the persistent identifier of this codec.
     *
     * @return the unique, case-sensitive engine identifier
     * @implSpec Keep this identifier stable across releases so persisted engine
     * selections continue to resolve.
     */
    String id();

    /**
     * Encodes a password for credential storage.
     *
     * @param password the supplied password
     * @return a non-blank, self-contained encoded hash
     * @implSpec Use a one-way password hashing algorithm and include any salt
     * and parameters needed for verification in the returned value.
     */
    String hash(String password);

    /**
     * Checks a candidate password against a stored hash.
     *
     * @param password the candidate password
     * @param hash the value previously returned by {@link #hash(String)}
     * @return {@code true} if the candidate matches
     */
    boolean matches(String password, String hash);
}
