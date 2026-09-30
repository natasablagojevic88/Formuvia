package rs.formuvia.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.model.entity.ModelFile;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(ModelFile.class)
public class ModelFileDTO {

	private UUID id;

	private String mimeType;

	private String fileName;

	private String path;

}
