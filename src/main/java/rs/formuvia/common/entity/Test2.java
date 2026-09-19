package rs.formuvia.common.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proba2")
@Setter
@Getter
public class Test2 {

	@Id
	private UUID id;

	@Column
	private String tekst2;

	@Column
	private LocalDate datum2;
	
	@JoinColumn(name="test3_id",foreignKey = @ForeignKey(name="fk_test2_test3"))
	private Test3 test3;
}
