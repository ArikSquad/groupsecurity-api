package eu.mikart.groupsecurity.antibot.model;

import org.jetbrains.annotations.NotNull;

public record TrustModifier(
    @NotNull ReasonCode reasonCode,
    int amount,
    @NotNull String evidence,
    @NotNull String source
) {
}
