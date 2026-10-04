package rs.formuvia.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.model.entity.ModelFileVersion;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(ModelFileVersion.class)
public class ModelFileVersionDTO {

	@HideInTable
	private UUID id;

	@HideInTable
	private String mimeType;

	private String fileName;

	@HideInTable
	private String path;

	@HideInTable
	private UUID modelFileId;

	private Integer version;

	private LocalDateTime creationDate;
}
