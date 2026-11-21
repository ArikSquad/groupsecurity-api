package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

/**
 * A trust adjustment used when evaluating a connection.
 *
 * @param reasonCode machine-readable reason for the adjustment
 * @param amount points subtracted from the risk score
 * @param evidence short explanation suitable for diagnostics
 * @param source stable identifier for the component that produced the adjustment
 */
public record TrustModifier(
    @NotNull ReasonCode reasonCode,
    int amount,
    @NotNull String evidence,
    @NotNull String source
) {
}
