package rs.formuvia.common.service;

import rs.formuvia.common.dto.LoginDTO;

public interface LoginService {

	void login(LoginDTO loginDTO);

	void logout();
}
