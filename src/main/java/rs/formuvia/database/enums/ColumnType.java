package rs.formuvia.database.enums;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public enum ColumnType {

	STRING(String.class), BOOLEAN(Boolean.class), BIGDECIMAL(BigDecimal.class), LONG(Long.class),
	INTEGER(Integer.class), LOCALDATE(LocalDate.class), LOCALDATETIME(LocalDateTime.class), UUID(UUID.class);

	public Class<?> typeClass;

	private ColumnType(Class<?> typeClass) {
		this.typeClass = typeClass;
	}

}
