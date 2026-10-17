package rs.formuvia.model.dto;

import java.util.UUID;

import com.drew.lang.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@NotNull
@AllArgsConstructor
public class ModelPreviewProcedureDTO {

	private UUID id;

	private String name;

	private String icon;
}
