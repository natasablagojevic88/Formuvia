package rs.formuvia.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.ColumnType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeDTO {

	private String fieldName;

	private ColumnType columnType;

	private Object oldData;

	private Object newData;
}
