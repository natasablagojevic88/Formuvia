package rs.formuvia.common.service;

import java.io.File;

import rs.formuvia.common.dto.FileUploadDTO;

public interface FileUploadService {

	FileUploadDTO uploadFile(File file);
}
