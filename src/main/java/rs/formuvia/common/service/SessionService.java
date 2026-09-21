package rs.formuvia.common.service;

import rs.formuvia.common.dto.ChangePasswordDTO;
import rs.formuvia.common.dto.UserInfoDTO;

public interface SessionService {

	UserInfoDTO getUserInfo();

	void changePassword(ChangePasswordDTO changePasswordDTO);
}
