package rs.formuvia.common.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.JobListDTO;
import rs.formuvia.common.service.JobService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Jobs", description = "Jobs of the schedule: the list with the state of the last run, one job by identifier and running a job by hand. All endpoints require a valid session; a job is shown only to a user who has the role the job is tied to, and a job without a role only to the admin role.")
public class JobController {

	@Inject
	private JobService jobService;

	@Path(ApiRoute.jobList)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Operation(summary = "List jobs", description = "Returns every job of the schedule the current user may see, all at once and without paging. One job is one row: name (the name it stands under in the schedule), active (whether it has a trigger), previousFire and nextFire (when it last ran and when it runs next, by the schedule), status, lastEndDate and errorText (from the last run that was written down), jobId and reportId. A job that has never run comes back without a status, and one whose report was deleted without a reportId.", responses = {
			@ApiResponse(responseCode = "200", description = "Jobs of the schedule", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = JobListDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response listJob() {
		return Response.ok(jobService.getList(null)).build();
	}

	@Path(ApiRoute.jobId)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Operation(summary = "Get job", description = "Returns one job of the schedule by its identifier, in the same form as the list. It is used to read a single row again after the job was run by hand or changed, without loading the whole list.", responses = {
			@ApiResponse(responseCode = "200", description = "Job found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JobListDTO.class))),
			@ApiResponse(responseCode = "400", description = "No job with that identifier, or the current user may not see it", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getJob(
			@Parameter(description = "Identifier of the job", required = true) @PathParam("id") UUID id) {
		return Response.ok(jobService.getJob(id)).build();
	}

	@Path(ApiRoute.jobExecuteId)
	@POST
	@Operation(summary = "Run job now", description = "Runs the job once, right away; its timetable stays as it is and the next run by the schedule is not moved. The answer comes back as soon as the schedule takes the job over, not when the job is done, so the outcome is read afterwards from the job itself. Nothing is returned.", responses = {
			@ApiResponse(responseCode = "204", description = "The job was handed over to the schedule"),
			@ApiResponse(responseCode = "400", description = "No job with that identifier, the job does not stand in the schedule, or the schedule is not running", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getExecute(
			@Parameter(description = "Identifier of the job to run", required = true) @PathParam("id") UUID id) {
		jobService.getExecute(id);
		return Response.noContent().build();
	}

	@Path(ApiRoute.jobLogsId)
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "List runs of a job", description = "Returns one page of the runs written down for the job with the given identifier, newest first. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = JobLogDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); the filter by the job is added by the server, so a filter on jobItemId is not needed. Response: name (translated table title), column (columns to show), allColumns (every JobLogDTO field with its translated label), list (runs in the JobLogDTO format: startTime, endTime, status and errorText), total and numberOfPages. A run that is still going has no endTime, and one that went through has no errorText. The runs are shown only to a user who may see the job itself.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of runs of the job", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "No job with that identifier, the current user may not see it, or a filter or sort names an unknown field", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getJobLogs(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the job whose runs are returned", required = true) @PathParam("id") UUID id) {

		return Response.ok(jobService.getJobLogs(id, databaseParameter)).build();
	}
}
