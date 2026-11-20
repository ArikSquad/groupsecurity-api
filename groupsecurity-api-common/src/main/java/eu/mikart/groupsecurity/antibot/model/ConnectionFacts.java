package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ConnectionFacts(
    @Nullable UUID playerId,
    @NotNull String username,
    @NotNull String ipAddress,
    @NotNull String subnetKey,
    @NotNull String ipHash,
    @NotNull String subnetHash,
    long timestamp,
    boolean proxied,
    boolean anonymousConnection,
    @Nullable String countryCode,
    @Nullable String countryName
) {
}
