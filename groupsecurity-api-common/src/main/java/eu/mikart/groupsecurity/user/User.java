package eu.mikart.groupsecurity.user;

import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * A stored player identity. Equality and hashing use the UUID so the
 * identity remains stable across username changes.
 */
@EqualsAndHashCode(exclude = {"name"})
public class User {

	private final UUID uuid;
	private final String name;

	/**
	 * Creates a player identity.
	 *
	 * @param uuid the persistent player identifier
	 * @param name the stored username
	 */
	public User(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	/**
	 * Returns the persistent player identifier.
	 *
	 * @return the player identifier
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Returns the username stored for this player.
	 *
	 * @return the stored username
	 */
	public String getName() {
		return name;
	}
}
