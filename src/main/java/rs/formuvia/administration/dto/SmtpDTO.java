package rs.formuvia.administration.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.common.enums.SmtpSecurity;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Smtp.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.smtp)
public class SmtpDTO {

	@HideInTable
	private UUID id;

	@NotNull
	@InitSort
	private String name;

	@NotNull
	private String sender;

	@NotNull
	private String host;

	@NotNull
	private Integer port;

	@NotNull
	private Boolean auth;

	@HideInTable
	private String username;

	@HideInTable
	private String password;

	@NotNull
	@HideInTable
	private SmtpSecurity smtpSecurity;

}
