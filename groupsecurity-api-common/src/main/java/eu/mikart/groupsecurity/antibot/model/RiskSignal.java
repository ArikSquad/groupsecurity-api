package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

/**
 * A weighted observation that contributes to a connection’s risk score.
 *
 * @param reasonCode machine-readable reason for the observation
 * @param weight points added to risk
 * @param evidence short explanation suitable for diagnostics
 * @param source stable identifier for the component that produced the signal
 */
public record RiskSignal(
    @NotNull ReasonCode reasonCode,
    int weight,
    @NotNull String evidence,
    @NotNull String source
) {
}
