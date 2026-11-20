package eu.mikart.groupsecurity.api;

/** An implementation must be safe to use from concurrent login requests. */
public interface PasswordCodec {
    String id();

    /** Encodes a password as a one-way hash for storage. */
    String hash(String password);

    /** Checks a candidate against the stored hash. */
    boolean matches(String password, String hash);
}
