package rs.formuvia.exceptions;

import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExcelImportException extends WebApplicationException {

	private static final long serialVersionUID = 1L;
	private final Integer rowNumber;
	private final Exception inException;

	public ExcelImportException(Integer rowNumber, Exception inException) {
		super("excelImportError");
		this.rowNumber = rowNumber;
		this.inException = inException;
	}

}
