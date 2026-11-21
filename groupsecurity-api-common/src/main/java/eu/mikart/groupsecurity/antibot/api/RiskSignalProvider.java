package eu.mikart.groupsecurity.antibot.api;

import eu.mikart.groupsecurity.antibot.model.BehaviorSnapshot;
import eu.mikart.groupsecurity.antibot.model.ConnectionFacts;
import eu.mikart.groupsecurity.antibot.model.RiskSignal;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Supplies additional observations during pre-login and post-login evaluation.
 * <p>Callbacks may run concurrently on the connection path. Implementations
 * should return promptly, use thread-safe state, and avoid blocking I/O.
 *
 * @see eu.mikart.groupsecurity.api.GroupSecurityAPI
 */
@FunctionalInterface
public interface RiskSignalProvider {

    /**
     * Evaluates the supplied connection snapshot.
     *
     * @param facts network and identity information for the connection
     * @param snapshot recent behavior, or {@link BehaviorSnapshot#EMPTY} when unavailable
     * @return the signals for this evaluation, or an empty list
     * @implSpec Return a non-null list containing only non-null elements.
     */
    @NotNull
    List<RiskSignal> compute(@NotNull ConnectionFacts facts, @NotNull BehaviorSnapshot snapshot);

}
