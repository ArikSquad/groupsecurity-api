package eu.mikart.groupsecurity.antibot.model;

public record BehaviorSnapshot(
	int chatMessagesLastWindow,
		int commandsLastWindow,
		int reconnectsLastWindow,
		boolean challengeAbandonRecently
) {
	public static final BehaviorSnapshot EMPTY = new BehaviorSnapshot(0, 0, 0, false);
}
