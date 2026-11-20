package eu.mikart.groupsecurity.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The GroupSecurity API implementation for the Velocity platform
 * <p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class GroupSecurityVelocityAPI extends GroupSecurityAPI {
    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     */
    @ApiStatus.Internal
    private GroupSecurityVelocityAPI(@NotNull Backend backend) {
        super(backend);
    }

    /**
     * Entrypoint to the GroupSecurity API on the Velocity platform - returns an instance of the API
     *
     * @return instance of the GroupSecurity API
     * @since 1.0
     */
    @NotNull
    public static GroupSecurityVelocityAPI getInstance() {
        if (GroupSecurityAPI.getInstance() instanceof GroupSecurityVelocityAPI current) return current;
        throw new NotRegisteredException();
    }

    /**
     * <b>(Internal use only)</b> - Register the API for this platform.
     *
     * @param backend the runtime adapter
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull Backend backend) {
        instance = new GroupSecurityVelocityAPI(backend);
    }
}
