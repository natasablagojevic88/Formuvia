package rs.formuvia.common.dto;

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
public class UserInfoDTO {

	private String username;

	private String name;

	private String surname;

	private List<MenuDTO> menu = new ArrayList<>();

}
