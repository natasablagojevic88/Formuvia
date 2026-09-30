package rs.formuvia.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUser;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_upload", uniqueConstraints = @UniqueConstraint(columnNames = {
		"path" }, name = "file_upload_path_unique"))
@Entity
public class FileUpload {

	@Id
	private UUID id;

	@Column(nullable = false, columnDefinition = "text")
	private String path;

	@JoinColumn(name = "app_user", nullable = false, foreignKey = @ForeignKey(name = "fk_file_upload_app_user"))
	private AppUser appUser;

	@Column(name = "creation_time", nullable = false)
	private LocalDateTime creationTime;
}
