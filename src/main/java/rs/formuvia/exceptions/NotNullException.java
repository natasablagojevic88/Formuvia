package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NotNullException extends WebApplicationException{

	private static final long serialVersionUID = 1L;

	private Class<?> fieldClass;
	
	private String field;

	public NotNullException(Class<?> fieldClass, String field) {
		super("fieldRequired");
		this.fieldClass = fieldClass;
		this.field = field;
	}
	
}
