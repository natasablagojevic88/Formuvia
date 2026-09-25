package rs.formuvia.model.controller;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.service.ModelColumnService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Path("")
public class ModelColumnController {

	@Inject
	private ModelColumnService modelColumnService;

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.modelColumnTable)
	public Response getList(@PathParam("modelId") UUID modelId) {
		return Response.ok(modelColumnService.getList(modelId)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.modelColumnId)
	public Response getModelColumn(@PathParam("id") UUID id) {
		return Response.ok(modelColumnService.getModelColumn(id)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Path(ApiRoute.modelColumn)
	public Response getUpdate(@Valid ModelColumnDTO modelColumnDTO) {
		return Response.ok(modelColumnService.getUpdate(modelColumnDTO)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Path(ApiRoute.modelColumnId)
	public Response getDelete(@PathParam("id") UUID id) {
		modelColumnService.getDelete(id);
		return Response.noContent().build();
	}
}
