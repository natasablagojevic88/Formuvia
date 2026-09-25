package rs.formuvia.exceptions;

import java.lang.reflect.Field;

import jakarta.validation.constraints.Max;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MaximumException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	private final Field field;
	private final Max max;

	public MaximumException(Field field, Max max) {
		super("maximumRequired");
		this.field = field;
		this.max = max;
	}

}
