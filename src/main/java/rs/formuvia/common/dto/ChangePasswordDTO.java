package rs.formuvia.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {

	@NotNull
	private String newPassword;
	
	@NotNull
	private String newPasswordAgain;
}
