package eu.mikart.groupsecurity.user;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"name"})
public class User {

	private final UUID uuid;
	private final String name;

}