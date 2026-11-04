package rs.formuvia.quartz.dto;

import java.util.UUID;

import com.drew.lang.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.quartz.entity.JobItemWebhook;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = JobItemWebhook.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.webhook)
public class JobItemWebhookDTO {

	@HideInTable
	private UUID id;

	@NotNull
	@HideInTable
	private UUID jobId;

	@HideInTable
	private UUID jobReportId;

	@InitSort
	private String bearerToken;

	private String description;

}
