package rs.formuvia.administration.service;

import java.util.UUID;

import rs.formuvia.administration.dto.SmtpDTO;
import rs.formuvia.administration.dto.TestMailDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface SmtpService {

	DatabaseTable<SmtpDTO> getTable(DatabaseParameter databaseParameter);

	SmtpDTO getSmtp(UUID id);

	SmtpDTO getUpdate(SmtpDTO smtpDTO);

	void getDelete(UUID id);

	void getSendTestMail(TestMailDTO testMailDTO, UUID smtpId);

}
