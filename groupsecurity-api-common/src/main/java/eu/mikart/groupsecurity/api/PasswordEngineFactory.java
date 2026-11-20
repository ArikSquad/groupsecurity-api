package eu.mikart.groupsecurity.api;

/** Creates a password codec. GroupSecurity owns credential storage and the login lifecycle. */
@FunctionalInterface
public interface PasswordEngineFactory {
    PasswordCodec create();
}
