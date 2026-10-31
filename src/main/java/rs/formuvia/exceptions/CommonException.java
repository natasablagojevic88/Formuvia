package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommonException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	private final Integer status;

	private final String inMessage;

	private final Object inObject;

	public CommonException(Integer status, String inMessage, Object inObject) {
		super(inMessage);
		this.status = status;
		this.inMessage = inMessage;
		this.inObject = inObject;
	}

}
