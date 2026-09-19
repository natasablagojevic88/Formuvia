package rs.formuvia.exceptions;

import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoDataFoundException extends WebApplicationException{

	private static final long serialVersionUID = 1L;
	
	private final UUID id;
	private final Class<?> inClass;
	public NoDataFoundException(UUID id, Class<?> inClass) {
		super("noDataFound");
		this.id = id;
		this.inClass = inClass;
	}
	
	

}
