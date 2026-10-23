package rs.formuvia.common.service.impl;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import jakarta.activation.DataHandler;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.common.entity.MailLog;
import rs.formuvia.common.service.SendMailService;
import rs.formuvia.common.utils.SendMailStatus;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.utils.StringUtils;

@Service
public class SendMailServiceImpl implements SendMailService {
	private final String TEXT_TYPE = "text/html; charset=UTF-8";
	private final String MILTIPART_TYPE = "mixed";
	private final String HOST = "mail.smtp.host";
	private final String PORT = "mail.smtp.port";
	private final String FROM = "mail.smtp.from";
	private final String AUTH = "mail.smtp.auth";
	private final String CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
	private final String TIMEOUT = "mail.smtp.timeout";
	private final String WRITE_TIMEOUT = "mail.smtp.writetimeout";
	private final String STARTTLS_ENABLE = "mail.smtp.starttls.enable";
	private final String STARTTLS_REQUIRED = "mail.smtp.starttls.required";
	private final String SSL_ENABLE = "mail.smtp.ssl.enable";
	private final String CONNECTION_TIMEOUT_VALUE = "10000";
	private final String TIMEOUT_VALUE = "20000";
	private final String TRUE = Boolean.TRUE.toString();

	@Inject
	private DatabaseService databaseService;

	Logger logManager = LogManager.getLogger(getClass());

	@Override
	public SendMailStatus sendMail(Smtp smtp, String receive, String subject, String body, byte[] attachment,
			String fileName) {
		if (!StringUtils.hasText(receive) || !StringUtils.hasText(subject)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noReceiveOrBody", null);
		}

		SendMailStatus sendMailStatus = new SendMailStatus();
		MailLog mailLog = new MailLog();
		mailLog.setMailText(body);
		mailLog.setReceive(receive);
		mailLog.setSubject(subject);
		mailLog.setSmtp(smtp);

		try {
			MimeMessage message = new MimeMessage(createSession(smtp));
			message.setFrom(new InternetAddress(smtp.getSender()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receive, false));
			message.setSubject(subject, StandardCharsets.UTF_8.name());
			message.setSentDate(new Date());

			if (attachment == null) {
				message.setContent(body == null ? "" : body, TEXT_TYPE);
			} else {
				MimeMultipart multipart = new MimeMultipart(MILTIPART_TYPE);

				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(body, TEXT_TYPE);
				multipart.addBodyPart(htmlPart);

				MimeBodyPart filePart = new MimeBodyPart();
				filePart.setDataHandler(new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(attachment),
						UpdateObject.findMimeType(attachment))));
				filePart.setFileName(MimeUtility.encodeText(fileName == null ? UUID.randomUUID().toString() : fileName,
						StandardCharsets.UTF_8.displayName(), null));
				filePart.setDisposition(Part.ATTACHMENT);
				multipart.addBodyPart(filePart);

				message.setContent(multipart);
			}
			Transport.send(message);
			sendMailStatus.setSent(true);
			mailLog.setSendTime(LocalDateTime.now());
			mailLog.setSent(true);
		} catch (Exception e) {
			logManager.error(e.getMessage(), e);
			sendMailStatus.setSent(false);
			sendMailStatus.setError(e);
			mailLog.setSendTime(LocalDateTime.now());
			mailLog.setSent(false);
			mailLog.setErrorText(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()).stream()
					.map(a -> a.toString()).collect(Collectors.joining("\n")));
		}

		try {
			databaseService = databaseService == null ? new DatabaseServiceImpl() : databaseService;
			this.databaseService.save(mailLog);
		} catch (Exception ex) {
			logManager.error(ex.getMessage(), ex);
		}

		return sendMailStatus;
	}

	private Session createSession(Smtp smtp) {
		Properties properties = new Properties();
		properties.put(HOST, smtp.getHost());
		properties.put(PORT, String.valueOf(smtp.getPort()));
		properties.put(FROM, smtp.getSender());
		properties.put(AUTH, String.valueOf(Boolean.TRUE.equals(smtp.getAuth())));
		properties.put(CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_VALUE);
		properties.put(TIMEOUT, TIMEOUT_VALUE);
		properties.put(WRITE_TIMEOUT, TIMEOUT_VALUE);

		switch (smtp.getSmtpSecurity()) {
		case STARTTLS -> {
			properties.put(STARTTLS_ENABLE, TRUE);
			properties.put(STARTTLS_REQUIRED, TRUE);
		}
		case SSL -> properties.put(SSL_ENABLE, TRUE);
		default -> {
		}
		}

		if (Boolean.TRUE.equals(smtp.getAuth()))
			return Session.getInstance(properties, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(smtp.getUsername(), smtp.getPassword());
				}
			});

		return Session.getInstance(properties);
	}

	@Override
	public SendMailStatus sendMail(Smtp smtp, String receive, String subject, String body) {

		return sendMail(smtp, receive, subject, body, null, null);
	}

}
