package rs.formuvia.administration.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appuser_role", uniqueConstraints = @UniqueConstraint(columnNames = { "appuser",
		"role" }, name = "appuser_role_unique1"), indexes = @Index(columnList = "appuser", name = "appuser_role_index1"))
public class AppUserRole {

	@Id
	private UUID id;

	@JoinColumn(name = "appuser", nullable = false, foreignKey = @ForeignKey(name = "fk_appuser_role_user"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private AppUser appUser;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_appuser_role_role"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Role role;
}
