package rs.formuvia.common.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.common.dto.FileUploadDTO;
import rs.formuvia.common.entity.FileUpload;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.FileUploadService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class FileUploadServiceImpl implements FileUploadService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private CommonService commonService;

	public static final String PATH_FILE_PARAMETER = "files.path";
	private final String FOLDER_DATE_FORMAT = "/yyyy/MM/dd";

	@Override
	public FileUploadDTO uploadFile(File file) {
		if (!file.exists()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFile", file.getName());
		}

		String filePath = StaticData.appProperties.getProperty(PATH_FILE_PARAMETER);
		if (!StringUtils.hasText(filePath)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFilePathInProperties",
					PATH_FILE_PARAMETER);
		}

		File rootFile = new File(filePath);
		if ((!rootFile.exists()) || (!rootFile.isDirectory())) {
			rootFile.mkdirs();
		}

		String folderPathSufix = LocalDate.now().format(DateTimeFormatter.ofPattern(FOLDER_DATE_FORMAT));

		File fullPathFolder = new File(rootFile.getAbsolutePath() + folderPathSufix);
		if ((!fullPathFolder.exists()) || (!fullPathFolder.isDirectory())) {
			fullPathFolder.mkdirs();
		}

		Boolean create = true;
		File createdFile = null;
		String fileName = null;
		while (create) {
			fileName = UUID.randomUUID().toString();
			createdFile = new File(fullPathFolder.getAbsolutePath() + "/" + fileName);
			if (!createdFile.exists()) {
				create = false;
			}
		}

		try {
			Files.move(Paths.get(file.getAbsolutePath()), Paths.get(createdFile.getAbsolutePath()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}

		FileUpload fileUpload = new FileUpload();
		fileUpload.setAppUser(commonService.getUser());
		fileUpload.setPath(folderPathSufix + "/" + fileName);
		fileUpload.setCreationTime(LocalDateTime.now());
		fileUpload = this.databaseService.save(fileUpload);

		return new FileUploadDTO(fileUpload.getId(), null, null, null);
	}

}
