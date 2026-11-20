package eu.mikart.groupsecurity.antibot.api;

import eu.mikart.groupsecurity.antibot.model.BehaviorSnapshot;
import eu.mikart.groupsecurity.antibot.model.ConnectionFacts;
import eu.mikart.groupsecurity.antibot.model.RiskSignal;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RiskSignalProvider {

    @NotNull
    List<RiskSignal> compute(@NotNull ConnectionFacts facts, @NotNull BehaviorSnapshot snapshot);

}
