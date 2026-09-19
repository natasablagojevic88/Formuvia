package rs.formuvia.administration.entity;

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
@Table(name="role",
	uniqueConstraints = @UniqueConstraint(columnNames = { "code" },name = "role_unique")
		)
public class Role {

	@Id
	private UUID id;
	
	@Column(nullable = false)
	private String code;
	
	@Column
	private String description;
}
