package rs.formuvia.common.service;

import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.common.utils.SendMailStatus;

public interface SendMailService {

	SendMailStatus sendMail(Smtp smtp, String receive, String subject, String body);

	SendMailStatus sendMail(Smtp smtp, String receive, String subject, String body, byte[] attachment, String fileName);
}
