package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;

public class WrongLoginException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	public WrongLoginException() {
		super("wrongLogin");
	}

}
