package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A snapshot of the host’s recorded trust history for a player.
 *
 * @param playerId player identifier
 * @param trustScore accumulated trust points
 * @param cleanJoinCount number of joins credited to the player’s trust history
 * @param lastSeenAt last observation in epoch milliseconds, or zero when unknown
 * @param lastKnownIpHash keyed hash of the last known IP address, or an empty string when unknown
 * @param lastKnownSubnetHash keyed hash of the last known network, or an empty string when unknown
 */
public record TrustProfile(
    @NotNull UUID playerId,
    int trustScore,
    int cleanJoinCount,
    long lastSeenAt,
    @NotNull String lastKnownIpHash,
    @NotNull String lastKnownSubnetHash
) {
}
