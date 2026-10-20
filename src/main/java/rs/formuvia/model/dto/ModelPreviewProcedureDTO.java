package rs.formuvia.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModelPreviewProcedureDTO {

	private UUID id;

	private String name;

	private String icon;
}
