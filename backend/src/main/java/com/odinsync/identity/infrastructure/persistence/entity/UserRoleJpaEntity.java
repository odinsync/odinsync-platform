package com.odinsync.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleJpaEntity.UserRoleId.class)
public class UserRoleJpaEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Id
	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Getter
	@Setter
	public static class UserRoleId implements Serializable {
		private UUID userId;
		private UUID roleId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof UserRoleId that)) {
				return false;
			}
			return Objects.equals(userId, that.userId)
					&& Objects.equals(roleId, that.roleId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, roleId);
		}
	}
}
