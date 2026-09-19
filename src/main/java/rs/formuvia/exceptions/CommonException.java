package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class CommonException extends WebApplicationException{

	private static final long serialVersionUID = 1L;

	private final Integer status;
	
	private final String inMessage;
	
	private final Object inObject;

	
	
}
