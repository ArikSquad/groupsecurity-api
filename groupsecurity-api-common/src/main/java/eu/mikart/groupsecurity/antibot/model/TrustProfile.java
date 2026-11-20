package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record TrustProfile(
    @NotNull UUID playerId,
    int trustScore,
    int cleanJoinCount,
    long lastSeenAt,
    @NotNull String lastKnownIpHash,
    @NotNull String lastKnownSubnetHash
) {
}
