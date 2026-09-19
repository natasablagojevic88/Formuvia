package rs.formuvia.common.service;

import rs.formuvia.common.dto.ChangePasswordDTO;
import rs.formuvia.common.dto.UserInfo;

public interface SessionService {

	UserInfo getUserInfo();

	void changePassword(ChangePasswordDTO changePasswordDTO);
}
