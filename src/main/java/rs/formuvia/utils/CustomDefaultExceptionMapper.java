package rs.formuvia.utils;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.LoginServiceImpl;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.ForbiddenException;
import rs.formuvia.exceptions.MaximumException;
import rs.formuvia.exceptions.MinimumException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UnAuthorizedException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.exceptions.WrongLoginException;

@Provider
public class CustomDefaultExceptionMapper implements ExceptionMapper<Throwable> {

	@Context
	private HttpServletRequest httpServletRequest;

	private Logger logger = LogManager.getLogger(getClass());

	@Context
	private HttpServletResponse httpServletResponse;

	@Inject
	private ResourceBundleService resourceBundleService;

	@Override
	public Response toResponse(Throwable exception) {
		Exception lastException = getLastException(exception);
		this.logger.error(this.httpServletRequest.getPathInfo());

		if (lastException.getClass().equals(NotNullException.class)) {
			return toNotNullException((NotNullException) lastException);
		}

		if (lastException.getClass().equals(WrongLoginException.class)) {
			return toWrongLoginException((WrongLoginException) lastException);
		}

		if (lastException.getClass().equals(UnAuthorizedException.class)) {
			return toUnauthorizedException((UnAuthorizedException) lastException);
		}

		if (lastException.getClass().equals(ForbiddenException.class)) {
			return toForbiddenException((ForbiddenException) lastException);
		}

		if (lastException.getClass().equals(CommonException.class)) {
			return toCommonException((CommonException) lastException);
		}

		if (lastException.getClass().equals(UniqueException.class)) {
			return toUniqueException((UniqueException) lastException);
		}

		if (lastException.getClass().equals(MinimumException.class)) {
			return toMinimumException((MinimumException) lastException);
		}

		if (lastException.getClass().equals(MaximumException.class)) {
			return toMaximumException((MaximumException) lastException);
		}

		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		errorDetail.setMessage(this.resourceBundleService.getText(lastException.getMessage()));
		this.logger.error(lastException.getMessage(), lastException);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(errorDetail).build();
	}

	private Response toWrongLoginException(WrongLoginException wrongLoginException) {
		this.logger.error(wrongLoginException.getMessage());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		errorDetail.setMessage(this.resourceBundleService.getText(wrongLoginException.getMessage()));
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toNotNullException(NotNullException notNullException) {
		this.logger.error(
				notNullException.getMessage() + ":" + notNullException.getField().getDeclaringClass().getSimpleName()
						+ ":" + notNullException.getField().getName());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		String message = this.resourceBundleService.getText(notNullException.getMessage());
		message += ": ";
		message += this.resourceBundleService.getText(createFieldName(notNullException.getField()));
		errorDetail.setMessage(message);
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	public static String createFieldName(Field field) {
		return field.getDeclaringClass().getSimpleName() + "." + field.getName();
	}

	private Response toUnauthorizedException(UnAuthorizedException forbiddenException) {
		this.logger.error(forbiddenException.getMessage());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_UNAUTHORIZED);
		errorDetail.setMessage(this.resourceBundleService.getText(forbiddenException.getMessage()));
		String accessTokenName = StaticData.appProperties.getProperty(LoginServiceImpl.ACCESS_TOKEN_NAME).toString();
		this.httpServletResponse.addCookie(LoginServiceImpl.createCookie(accessTokenName, null, 0));
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toForbiddenException(ForbiddenException forbiddenException) {
		this.logger.error(forbiddenException.getMessage());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
		errorDetail.setMessage(this.resourceBundleService.getText(forbiddenException.getMessage()));
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toCommonException(CommonException commonException) {
		this.logger.error(commonException.getInMessage() + " : " + commonException.getInObject(), commonException);
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(commonException.getStatus());
		String message = this.resourceBundleService.getText(commonException.getInMessage());
		if (StringUtils.notNull(commonException.getInObject()))
			message += ": " + commonException.getInObject();
		errorDetail.setMessage(message);
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toUniqueException(UniqueException uniqueException) {
		this.logger.error(uniqueException.getMessage() + " : " + uniqueException.getField().getName() + " : "
				+ uniqueException.getData());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		String fieldName = this.resourceBundleService.getText(createFieldName(uniqueException.getField()));
		String mesage = fieldName + " " + this.resourceBundleService.getText(uniqueException.getMessage());
		mesage += ": " + uniqueException.getData();
		errorDetail.setMessage(mesage);
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toMinimumException(MinimumException minimumException) {
		this.logger.error(minimumException.getMessage() + " : " + minimumException.getField().getName() + " : "
				+ minimumException.getMin().value());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		String fieldName = this.resourceBundleService.getText(createFieldName(minimumException.getField()));
		String mesage = fieldName + " - " + this.resourceBundleService.getText(minimumException.getMessage());
		mesage += ": " + minimumException.getMin().value();
		errorDetail.setMessage(mesage);
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Response toMaximumException(MaximumException maximumException) {
		this.logger.error(maximumException.getMessage() + " : " + maximumException.getField().getName() + " : "
				+ maximumException.getMax().value());
		ErrorDetail errorDetail = new ErrorDetail();
		errorDetail.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
		String fieldName = this.resourceBundleService.getText(createFieldName(maximumException.getField()));
		String mesage = fieldName + " - " + this.resourceBundleService.getText(maximumException.getMessage());
		mesage += ": " + maximumException.getMax().value();
		errorDetail.setMessage(mesage);
		return Response.status(errorDetail.getStatus()).entity(errorDetail).build();
	}

	private Exception getLastException(Throwable exception) {
		Exception lastException = (Exception) exception;
		while (lastException.getCause() != null) {
			lastException = (Exception) lastException.getCause();
		}
		return lastException;
	}

}
