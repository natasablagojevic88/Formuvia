package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;

public class UnAuthorizedException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	public UnAuthorizedException() {
		super("unauthorized");
	}

}
