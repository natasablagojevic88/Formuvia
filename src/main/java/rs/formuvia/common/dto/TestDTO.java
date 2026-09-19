package rs.formuvia.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.entity.Test;
import rs.formuvia.common.enums.TestEnum;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.enums.Direction;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(Test.class)
public class TestDTO {

	@InitSort(orderNumber = 3)
	private UUID id;

	private String kratakTekst;

	@InitSort(orderNumber = 2,direction = Direction.DESC)
	private String dugTekst;

	private Long dugacakBroj;

	private BigDecimal decimalanBroj;

	private TestEnum enumtest;

	private TestEnum enumtest2;

	private LocalDate datum;

	private LocalDateTime datumVreme;

	private UUID test2Id;
	
	private String test2Tekst2;
	
	@InitSort(orderNumber = 1,direction = Direction.ASC)
	private LocalDate test2Datum2;
	
	private UUID test2Test3Id;
	
	private String test2Test3Tekst3;
	
	private LocalDateTime test2Test3DatumVreme3;
	
	private Boolean test2Test3Dane;
}
