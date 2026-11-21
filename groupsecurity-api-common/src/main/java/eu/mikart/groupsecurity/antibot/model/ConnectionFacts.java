package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Network and identity information available during a connection evaluation.
 * <p>Network addresses and account identifiers may be sensitive. Keep them out of
 * provider evidence and logs unless your integration has an explicit need for them.
 *
 * @param playerId player identifier, or {@code null} if unknown
 * @param username username presented by the connection
 * @param ipAddress source IP address
 * @param subnetKey source network identifier used to group addresses
 * @param ipHash keyed hash of the source address
 * @param subnetHash keyed hash of the source network
 * @param timestamp time of the connection observation in epoch milliseconds
 * @param proxied whether the connection arrived through a proxy
 * @param anonymousConnection whether network enrichment identified an anonymous connection
 * @param countryCode country code, or {@code null} when unavailable
 * @param countryName display name of the country, or {@code null} when unavailable
 */
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
