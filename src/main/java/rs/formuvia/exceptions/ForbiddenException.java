package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;

public class ForbiddenException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	public ForbiddenException() {
		super("forbidden");
	}

}
