package rs.formuvia.database.utils;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSubTable {

	private String name;

	private UUID modelId;

	private String icon;
}
