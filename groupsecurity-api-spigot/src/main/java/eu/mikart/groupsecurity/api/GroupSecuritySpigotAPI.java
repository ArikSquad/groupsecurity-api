package eu.mikart.groupsecurity.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The GroupSecurity API binding for Spigot and compatible Bukkit servers.
 *
 * @see #getInstance()
 * @see GroupSecurityAPI
 */
@SuppressWarnings("unused")
public class GroupSecuritySpigotAPI extends GroupSecurityAPI {
    private GroupSecuritySpigotAPI(@NotNull Backend backend) {
        super(backend);
    }

    /**
     * Returns the active Spigot and compatible Bukkit servers binding.
     *
     * @return the registered platform API
     * @throws GroupSecurityAPI.NotRegisteredException if the binding is unavailable
     * @since 1.0
     */
    @NotNull
    public static GroupSecuritySpigotAPI getInstance() {
        if (GroupSecurityAPI.getInstance() instanceof GroupSecuritySpigotAPI current) return current;
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
        instance = new GroupSecuritySpigotAPI(backend);
    }
}
