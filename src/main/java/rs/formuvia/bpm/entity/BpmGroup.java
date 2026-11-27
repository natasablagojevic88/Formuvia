package rs.formuvia.bpm.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "bpm_group", uniqueConstraints = @UniqueConstraint(columnNames = {
		"name" }, name = "bpm_group_name_unique"))
public class BpmGroup {

	public BpmGroup(UUID id) {
		this.id = id;
	}

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

}
