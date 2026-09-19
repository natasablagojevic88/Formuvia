package rs.formuvia.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.enums.TrackAction;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="track",
	indexes = @Index(columnList = "table_name,data_id",name="track_index1")
		)
public class Track {

	@Id
	private UUID id;
	
	@Column(name="table_name",nullable = false)
	private String tableName;
	
	@Column(name="data_id",nullable = false)
	private UUID dataId;
	
	@Column(nullable = false)
	private TrackAction action;
	
	@Column(name="date_time", nullable = false, columnDefinition = "timestamp(6) default now()")
	private LocalDateTime dateTime;
	
	@JoinColumn(name="app_user",foreignKey = @ForeignKey(name="fk_track_app_user"),columnDefinition = "uuid default nullif(current_setting('app.app_user_id', true),'')::uuid")
	private AppUser appUser;
	
	@Column(name="ip_address",columnDefinition = "varchar(255) default nullif(current_setting('app.ip_address', true),'')")
	private String ipAddress;
	
	@Column(name="old_data", columnDefinition = "text")
	private String oldData;
	
	@Column(name="new_data", columnDefinition = "text")
	private String newData;
}
