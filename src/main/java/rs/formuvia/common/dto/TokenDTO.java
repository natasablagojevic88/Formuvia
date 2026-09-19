package rs.formuvia.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.entity.Token;
import rs.formuvia.database.annotations.EntityClass;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(Token.class)
public class TokenDTO {

	private UUID id;

	private UUID appUserId;

	private String accessToken;

	private LocalDateTime accessTokenExipires;

	private String refreshToken;

	private LocalDateTime refreshTokenExipires;

	private Boolean active = true;
}
