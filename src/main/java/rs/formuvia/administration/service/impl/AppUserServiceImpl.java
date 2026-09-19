package rs.formuvia.administration.service.impl;

import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.inject.Inject;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.service.AppUserService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.utils.CheckAdmin;
import rs.formuvia.utils.StringUtils;

@Service
public class AppUserServiceImpl implements AppUserService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public DatabaseTable<AppUserDTO> getTable(DatabaseParameter databaseParameter) {

		DatabaseTable<AppUserDTO> databaseTable = databaseService.createTable(databaseParameter, AppUserDTO.class);
		databaseTable.getList().forEach(a -> {
			a.setPassword(null);
		});
		return databaseTable;
	}

	@Override
	public AppUserDTO getAppUserDTO(UUID id) {
		AppUserDTO appUserDTO = databaseService.findById(id, AppUserDTO.class);
		appUserDTO.setPassword(null);
		return appUserDTO;
	}

	@Override
	public AppUserDTO getUpdate(AppUserDTO appUserDTO) {
		AppUser appUser = appUserDTO.getId() == null ? new AppUser()
				: this.databaseService.findById(appUserDTO.getId(), AppUser.class);
		if (appUserDTO.getId() == null && (!StringUtils.hasText(appUserDTO.getPassword()))) {
			throw new NotNullException(appUserDTO.getClass(), "password");
		}

		if (StringUtils.hasText(appUserDTO.getPassword())) {
			appUserDTO.setPassword(convertPasswordToHash(appUserDTO.getPassword()));
		} else {
			appUserDTO.setPassword(appUser.getPassword());
		}

		modelMapper.map(appUserDTO, appUser);

		appUser = this.databaseService.save(appUser);
		appUser.setPassword(null);

		return modelMapper.map(appUser, AppUserDTO.class);
	}

	public static String convertPasswordToHash(String password) {
		return BCrypt.withDefaults().hashToString(CheckAdmin.BCRYPT_CODE, password.toCharArray());
	}

	@Override
	public void getDelete(UUID uuid) {
		this.databaseService.delete(this.databaseService.findById(uuid, AppUser.class));

	}

}
