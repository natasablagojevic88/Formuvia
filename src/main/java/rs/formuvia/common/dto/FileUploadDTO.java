package rs.formuvia.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {

	private UUID fileUploadFile;

	private UUID id;

	private String fileName;

	private String mimeType;
}
