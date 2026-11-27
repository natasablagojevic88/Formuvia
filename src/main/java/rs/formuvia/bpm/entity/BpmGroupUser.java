package rs.formuvia.bpm.entity;

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
import rs.formuvia.administration.entity.AppUser;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bpm_group_user", uniqueConstraints = @UniqueConstraint(columnNames = { "appuser",
		"user_group" }, name = "bpm_group_user_unique"), indexes = @Index(columnList = "user_group", name = "bpm_group_user_index_group"))
public class BpmGroupUser {

	@Id
	private UUID id;

	@JoinColumn(name = "user_group", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_group_user_group"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmGroup userGroup;

	@JoinColumn(name = "appuser", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_group_user_appuser"))
	private AppUser appUser;
}
