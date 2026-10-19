package rs.formuvia.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.dto.FileUploadDTO;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.model.entity.ModelJasper;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = ModelJasper.class, roles = RoleList.ADMIN)
public class ModelJasperDTO {

	private UUID id;

	@NotNull
	private UUID modelId;

	@NotNull
	private String name;

	private UUID fileId;

	private String fileFileName;

	@SkipColumn
	private FileUploadDTO fileUpload;
}
