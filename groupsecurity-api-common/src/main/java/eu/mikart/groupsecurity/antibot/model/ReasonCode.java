package eu.mikart.groupsecurity.antibot.model;

/**
 * Stable reason identifiers for risk signals and trust modifiers. The host’s
 * configuration determines the associated weights.
 */
public enum ReasonCode {
	/** A burst of connection attempts from one IP address. */
	IP_BURST,
	/** A burst of connection attempts from one subnet. */
	SUBNET_BURST,
	/** Repeated reconnects by the same connection identity. */
	RECONNECT_LOOP,
	/** Several distinct usernames from one IP address. */
	SAME_IP_MANY_NAMES,
	/** Several distinct usernames from one subnet. */
	SAME_SUBNET_MANY_NAMES,
	/** Elevated activity associated with a subnet. */
	SAME_SUBNET_HOTNESS,
	/** A username with characteristics associated with automated accounts. */
	SUSPICIOUS_NAME_PATTERN,
	/** Recent denied connections associated with the same network. */
	RECENT_BLOCKS_NEARBY,
	/** A country classification that contributes to risk. */
	COUNTRY_RISK,
	/** An anonymous network, VPN, or similar proxy detected by enrichment. */
	VPN_DETECTED,
	/** A country denied by the configured access policy. */
	COUNTRY_BANNED,
	/** An autonomous system associated with elevated risk. */
	ASN_RISK,
	/** A high rate of player chat messages. */
	CHAT_SPAM,
	/** A high rate of player commands. */
	COMMAND_SPAM,
	/** A risk adjustment while emergency mode is active. */
	EMERGENCY_MODE,
	/** The first tier of clean join history. */
	CLEAN_HISTORY_7D,
	/** The second tier of clean join history. */
	CLEAN_HISTORY_30D,
	/** A connection from a previously observed network. */
	STABLE_HISTORY,
	/** An adjustment attributed to a staff decision. */
	STAFF_OVERRIDE
}
