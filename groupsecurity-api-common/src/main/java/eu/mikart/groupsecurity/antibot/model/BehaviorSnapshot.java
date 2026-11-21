package eu.mikart.groupsecurity.antibot.model;

/**
 * Observed player activity in the host’s current behavior window.
 *
 * @param chatMessagesLastWindow chat messages observed in the window
 * @param commandsLastWindow commands observed in the window
 * @param reconnectsLastWindow reconnects observed in the window
 * @param challengeAbandonRecently whether the player recently abandoned a challenge
 */
public record BehaviorSnapshot(
	int chatMessagesLastWindow,
	int commandsLastWindow,
	int reconnectsLastWindow,
	boolean challengeAbandonRecently
) {
	/**
	 * A snapshot with no activity and no recent challenge abandonment.
	 */
	public static final BehaviorSnapshot EMPTY = new BehaviorSnapshot(0, 0, 0, false);
}
