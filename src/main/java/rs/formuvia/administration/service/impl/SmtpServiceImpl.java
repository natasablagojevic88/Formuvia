package rs.formuvia.administration.service.impl;

import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.administration.dto.SmtpDTO;
import rs.formuvia.administration.dto.TestMailDTO;
import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.administration.service.SmtpService;
import rs.formuvia.common.service.SendMailService;
import rs.formuvia.common.utils.SendMailStatus;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class SmtpServiceImpl implements SmtpService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private SendMailService sendMailService;

	private ModelMapper modelMapper = new ModelMapper();

	private final String SUBJECT_TEST_MAIL = "test.mail.subject.text";

	@Override
	public DatabaseTable<SmtpDTO> getTable(DatabaseParameter databaseParameter) {
		DatabaseTable<SmtpDTO> databaseTable = databaseService.createTable(databaseParameter, SmtpDTO.class);
		databaseTable.getList().forEach(a -> a.setPassword(null));
		return databaseTable;
	}

	@Override
	public SmtpDTO getSmtp(UUID id) {
		Smtp smtp = this.databaseService.findById(id, Smtp.class);
		smtp.setPassword(null);
		return modelMapper.map(smtp, SmtpDTO.class);
	}

	@Override
	public SmtpDTO getUpdate(SmtpDTO smtpDTO) {
		Smtp smtp = StringUtils.isNull(smtpDTO.getId()) ? new Smtp()
				: this.databaseService.findById(smtpDTO.getId(), Smtp.class);

		DatabaseParameter databaseParameter = DatabaseParameter
				.singleFilter(DatabaseFilter.valueOf("name", smtpDTO.getName()));

		if (StringUtils.notNull(smtpDTO.getId()))
			databaseParameter.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, smtpDTO.getId().toString()));

		if (this.databaseService.exists(databaseParameter, SmtpDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(SmtpDTO.class, "name"), smtpDTO.getName());

		if (smtpDTO.getAuth()) {
			if (!StringUtils.hasText(smtpDTO.getUsername()))
				throw new NotNullException(UniqueException.findFieldFromList(SmtpDTO.class, "username"));

			if (StringUtils.isNull(smtpDTO.getId())) {
				if (!StringUtils.hasText(smtpDTO.getPassword()))
					throw new NotNullException(UniqueException.findFieldFromList(SmtpDTO.class, "password"));
			} else {
				if (!StringUtils.hasText(smtpDTO.getPassword()))
					if (!StringUtils.hasText(smtp.getPassword()))
						throw new NotNullException(UniqueException.findFieldFromList(SmtpDTO.class, "password"));
					else
						smtpDTO.setPassword(smtp.getPassword());
			}

		} else {
			smtpDTO.setUsername(null);
			smtpDTO.setPassword(null);
		}

		this.modelMapper.map(smtpDTO, smtp);
		smtp = this.databaseService.save(smtp);

		return getSmtp(smtp.getId());
	}

	@Override
	public void getDelete(UUID id) {
		Smtp smtp = this.databaseService.findById(id, Smtp.class);
		this.databaseService.delete(smtp);
	}

	@Override
	public void getSendTestMail(TestMailDTO testMailDTO, UUID smtpId) {
		Smtp smtp = this.databaseService.findById(smtpId, Smtp.class);
		SendMailStatus sendMailStatus = this.sendMailService.sendMail(smtp, testMailDTO.getEmail(),
				StaticData.appProperties.getProperty(SUBJECT_TEST_MAIL), null);

		if (!sendMailStatus.getSent()) {
			throw new WebApplicationException(sendMailStatus.getError());
		}

	}

}
