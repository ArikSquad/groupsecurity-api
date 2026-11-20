package eu.mikart.groupsecurity.antibot.api;

import eu.mikart.groupsecurity.antibot.model.ConnectionFacts;
import eu.mikart.groupsecurity.antibot.model.TrustModifier;
import eu.mikart.groupsecurity.antibot.model.TrustProfile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TrustModifierProvider {

    @NotNull
    List<TrustModifier> compute(@NotNull TrustProfile profile, @NotNull ConnectionFacts facts);

}
