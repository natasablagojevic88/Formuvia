package rs.formuvia.exceptions;

import java.lang.reflect.Field;

import jakarta.validation.constraints.Min;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class MinimumException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	private final Field field;
	private final Min min;

}
