package eu.mikart.groupsecurity.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The GroupSecurity API binding for Velocity.
 *
 * @see #getInstance()
 * @see GroupSecurityAPI
 */
@SuppressWarnings("unused")
public class GroupSecurityVelocityAPI extends GroupSecurityAPI {
    private GroupSecurityVelocityAPI(@NotNull Backend backend) {
        super(backend);
    }

    /**
     * Returns the active Velocity binding.
     *
     * @return the registered platform API
     * @throws GroupSecurityAPI.NotRegisteredException if the binding is unavailable
     * @since 1.0
     */
    @NotNull
    public static GroupSecurityVelocityAPI getInstance() {
        if (GroupSecurityAPI.getInstance() instanceof GroupSecurityVelocityAPI current) return current;
        throw new NotRegisteredException();
    }

    /**
     * Publishes the API binding during platform startup.
     *
     * @param backend the runtime adapter
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull Backend backend) {
        instance = new GroupSecurityVelocityAPI(backend);
    }
}
