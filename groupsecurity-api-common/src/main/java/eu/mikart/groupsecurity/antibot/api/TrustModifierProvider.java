package eu.mikart.groupsecurity.antibot.api;

import eu.mikart.groupsecurity.antibot.model.ConnectionFacts;
import eu.mikart.groupsecurity.antibot.model.TrustModifier;
import eu.mikart.groupsecurity.antibot.model.TrustProfile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Supplies trust adjustments during post-login evaluation.
 * <p>Callbacks may run concurrently on the connection path. Implementations
 * should return promptly, use thread-safe state, and avoid blocking I/O.
 *
 * @see eu.mikart.groupsecurity.api.GroupSecurityAPI
 */
@FunctionalInterface
public interface TrustModifierProvider {

    /**
     * Evaluates the supplied connection snapshot.
     *
     * @param profile the player’s current trust profile
     * @param facts network and identity information for the connection
     * @return the adjustments for this evaluation, or an empty list
     * @implSpec Return a non-null list containing only non-null elements.
     */
    @NotNull
    List<TrustModifier> compute(@NotNull TrustProfile profile, @NotNull ConnectionFacts facts);

}
