package rs.formuvia.common.service.impl;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.utils.UpdateAppUser;
import rs.formuvia.common.dto.ChangePasswordDTO;
import rs.formuvia.common.dto.MenuDTO;
import rs.formuvia.common.dto.UserInfoDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.SessionService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.MenuInfo;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class SessionServiceImpl implements SessionService {

	@Inject
	private CommonService commonService;

	@Inject
	private ResourceBundleService resourceBundleService;

	private final String PASSWORDS_NOT_EQUAL_MESSAGE = "passwordsNotEqual";

	@Inject
	private DatabaseService databaseService;

	private final String MODEL_NAME_TO_REPLACE = "{modelId}";

	@Override
	public UserInfoDTO getUserInfo() {
		AppUser appUser = commonService.getUser();
		UserInfoDTO userInfo = new UserInfoDTO();
		userInfo.setName(appUser.getName());
		userInfo.setSurname(appUser.getSurname());
		userInfo.setUsername(appUser.getUsername());

		Set<String> roles = commonService.getRoles();

		for (MenuInfo menuInfo : StaticData.menuInfos) {

			loadMenu(roles, userInfo, menuInfo, null);
		}

		loadMenuFromModel(userInfo);

		return userInfo;
	}

	private void loadMenuFromModel(UserInfoDTO userInfo) {

		List<ModelDTO> modelWithoutParent = StaticData.models.stream().filter(a -> StringUtils.isNull(a.getParentId()))
				.collect(Collectors.toList());
		for (ModelDTO modelDTO : modelWithoutParent) {
			MenuDTO menuDTO = new MenuDTO();
			menuDTO.setIcon(modelDTO.getIcon());
			menuDTO.setName(resourceBundleService.getText(modelDTO.getName()));
			loadModelChildren(modelDTO, menuDTO);
			if (!menuDTO.getChildren().isEmpty()) {
				userInfo.getMenu().add(menuDTO);
			}
		}
	}

	private void loadModelChildren(ModelDTO modelDTO, MenuDTO parentMenu) {
		List<ModelDTO> childrenModel = StaticData.models.stream().filter(a -> StringUtils.notNull(a.getParentId()))
				.filter(a -> a.getParentId().equals(modelDTO.getId()))
				.filter(a -> commonService.hasRole(a.getPreviewRoleCode())).collect(Collectors.toList());
		for (ModelDTO childModel : childrenModel) {
			MenuDTO menuDTO = new MenuDTO();
			menuDTO.setIcon(childModel.getIcon());
			menuDTO.setName(resourceBundleService.getText(childModel.getName()));
			menuDTO.setUrl(ApiRoute.modelPreviewTable.replace(MODEL_NAME_TO_REPLACE, childModel.getId().toString()));

			parentMenu.getChildren().add(menuDTO);
		}
	}

	private void loadMenu(Set<String> roles, UserInfoDTO userInfo, MenuInfo menuInfo, MenuDTO parent) {
		if (StringUtils.hasText(menuInfo.getRole()) && !roles.contains(menuInfo.getRole())) {
			return;
		}
		MenuDTO menuDTO = new MenuDTO();
		menuDTO.setIcon(menuInfo.getIcon());
		menuDTO.setName(resourceBundleService.getText(menuInfo.getName()));
		menuDTO.setUrl(menuInfo.getUrl());

		for (MenuInfo child : menuInfo.getItems()) {
			loadMenu(roles, userInfo, child, menuDTO);
		}

		if (parent == null)
			userInfo.getMenu().add(menuDTO);
		else
			parent.getChildren().add(menuDTO);
	}

	@Override
	public void changePassword(ChangePasswordDTO changePasswordDTO) {
		if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getNewPasswordAgain())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, PASSWORDS_NOT_EQUAL_MESSAGE, null);
		}

		AppUser appUser = this.databaseService.findById(commonService.getUser().getId(), AppUser.class);
		appUser.setPassword(UpdateAppUser.convertPasswordToHash(changePasswordDTO.getNewPassword()));
		this.databaseService.save(appUser);
	}

}
