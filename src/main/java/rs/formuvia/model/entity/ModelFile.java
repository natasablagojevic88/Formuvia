package rs.formuvia.model.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_file", uniqueConstraints = @UniqueConstraint(columnNames = {
		"path" }, name = "model_file_path_unique"))
public class ModelFile {

	@Id
	private UUID id;

	@Column(name = "mime_type", nullable = false)
	private String mimeType;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String path;
}
