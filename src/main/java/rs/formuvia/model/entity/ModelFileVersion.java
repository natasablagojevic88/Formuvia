package rs.formuvia.model.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
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
@Table(name = "model_file_version", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "path" }, name = "model_file_version_path_unique"),
		@UniqueConstraint(columnNames = { "version",
				"model_file" }, name = "model_path_unique2") }, indexes = @Index(columnList = "model_file", name = "model_file_version_index1"))
public class ModelFileVersion {

	@Id
	private UUID id;

	@Column(name = "mime_type", nullable = false)
	private String mimeType;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String path;

	@JoinColumn(nullable = false, name = "model_file", foreignKey = @ForeignKey(name = "fk_model_file_version_model_file"))
	@ManyToMany(cascade = CascadeType.REMOVE)
	private ModelFile modelFile;

	@Column(nullable = false)
	private Integer version;
}
