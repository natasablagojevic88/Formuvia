package rs.formuvia.exceptions;

import java.lang.reflect.Field;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NotNullException extends WebApplicationException{

	private static final long serialVersionUID = 1L;

	private Field field;


	public NotNullException(Field field) {
		super("fieldRequired");
		this.field = field;
	}
	
}
