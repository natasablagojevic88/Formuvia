package rs.formuvia.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "test3")
public class Test3 {

	@Id
	private UUID id;

	@Column
	private String tekst3;

	@Column(name = "datum_vreme3")
	private LocalDateTime datumVreme3;

	@Column
	private Boolean dane;

}
