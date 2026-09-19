package rs.formuvia.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginDTO {

	@NotNull
	private String username;

	@NotNull
	private String password;
}
