package rs.formuvia.common.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import rs.formuvia.common.enums.TestEnum;

@Entity
@Table(name="test",
	uniqueConstraints = {@UniqueConstraint(name="test_unique", columnNames = { "kratak_tekst" }),
			@UniqueConstraint(name="test_unique2", columnNames = { "kratak_tekst","dugacak_broj" })
},indexes = { @Index(name="test_index1", columnList = "dug_tekst") }
		)
@Setter
@Getter
public class Test {

	@Id
	private UUID id;
	
	@Column(name="kratak_tekst",nullable = false)
	private String kratakTekst;
	
	@Column(name="dug_tekst",columnDefinition = "text")
	private String dugTekst;
	
	@Column(name="dugacak_broj")
	private Long dugacakBroj;
	
	@Column(name="decimalan_broj",scale = 3)
	private BigDecimal decimalanBroj;
	
	@Column
	private TestEnum enumtest;
	
	@Column
	private TestEnum enumtest2;
	
	@Column
	private LocalDate datum;
	
	@Column(name="datum_vreme")
	private LocalDateTime datumVreme;
	
	@JoinColumn(foreignKey = @ForeignKey(name="fk_test_test2"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Test2 test2;
}
