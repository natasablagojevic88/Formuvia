package rs.formuvia.common.controller;

import java.io.File;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.FileUploadDTO;
import rs.formuvia.common.service.FileUploadService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Files", description = "Storing files that belong to records. A file is sent on its own, before the record that will hold it is saved: the answer carries the identifier under which the file was stored, and that identifier is what goes into a column of type FILE.")
public class FileUploadController {

	@Inject
	private FileUploadService fileUploadService;

	@POST
	@Path(ApiRoute.fileUpload)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "uploadFile", summary = "Store a file", description = "Takes the raw content of one file as the request body and stores it under the folder given by files.path in the settings, in a subfolder of the current date, under a name of its own so that two files never collide. The record of the upload keeps the path, the user who sent it and the time. The answer is the identifier of that record, which the client writes into a column of type FILE when it saves the record the file belongs to. Requires a valid session.", requestBody = @RequestBody(description = "Content of the file", required = true, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)), responses = {
			@ApiResponse(responseCode = "200", description = "Identifier of the stored file", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileUploadDTO.class))),
			@ApiResponse(responseCode = "400", description = "No file in the request, or files.path is not set in the settings", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUploadFile(File file) {

		return Response.ok(fileUploadService.uploadFile(file)).build();
	}
}
