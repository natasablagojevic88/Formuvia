package rs.formuvia.exceptions;

import java.lang.reflect.Field;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;
import rs.formuvia.utils.StaticData;

@Setter
@Getter
public class UniqueException extends WebApplicationException {

	private static final long serialVersionUID = 1L;

	private final Field field;
	private final String data;

	public UniqueException(Field field, String data) {
		super("dataAlreadyExists");
		this.field = field;
		this.data = data;
	}

	public static Field findFieldFromList(Class<?> inClass, String field) {
		return StaticData.classFields.get(inClass).stream().filter(a -> a.getName().equals(field)).findFirst().get();
	}

}
