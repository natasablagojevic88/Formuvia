package rs.formuvia.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDTO {

	private String appUserUsername;

	private String appUserName;

	private String appUserSurname;

	private String action;

	private LocalDateTime time;

	List<ChangeDTO> changes = new ArrayList<>();

}
