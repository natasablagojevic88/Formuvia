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
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Path("")
public class ModelController {

	@Inject
	private ModelService modelService;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelTree)
	@RolesAllowed(RoleList.ADMIN)
	public Response getTree(
			) {
		return Response.ok(modelService.getTree()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	public Response getModel(
			@PathParam("id") UUID id
			) {
		return Response.ok(modelService.getModel(id)).build();
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.model)
	@RolesAllowed(RoleList.ADMIN)
	public Response getUpdate(
			@Valid ModelDTO modelDTO
			) {
		return Response.ok(modelService.getUpdate(modelDTO)).build();
	}
	
	@DELETE
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	public Response getDelete(
			@PathParam("id") UUID id
			) {
		modelService.getDelete(id);
		return Response.noContent().build();
	}
	
}
