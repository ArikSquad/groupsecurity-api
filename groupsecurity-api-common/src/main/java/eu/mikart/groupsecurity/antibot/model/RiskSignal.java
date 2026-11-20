package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

public record RiskSignal(
    @NotNull ReasonCode reasonCode,
    int weight,
    @NotNull String evidence,
    @NotNull String source
) {
}
