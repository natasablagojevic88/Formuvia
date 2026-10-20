package rs.formuvia.model.utils;

import java.io.File;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.Arrays;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

import jakarta.servlet.http.HttpServletRequest;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import rs.formuvia.common.entity.FileUpload;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.dto.ModelJasperDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelJasper;
import rs.formuvia.utils.StringUtils;

public class UpdateModelJasper implements ExecuteQuery<ModelJasperDTO> {
	private final HttpServletRequest httpServletRequest;
	private final ModelJasperDTO modelJasperDTO;

	private static DatabaseService databaseService = new DatabaseServiceImpl();
	private CommonService commonService;
	private ModelMapper modelMapper = new ModelMapper();

	public UpdateModelJasper(HttpServletRequest httpServletRequest, ModelJasperDTO modelJasperDTO) {
		this.httpServletRequest = httpServletRequest;
		this.modelJasperDTO = modelJasperDTO;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public ModelJasperDTO execute(Connection connection) throws Exception {

		ModelJasper modelJasper = StringUtils.isNull(this.modelJasperDTO.getId()) ? new ModelJasper()
				: databaseService.findById(this.modelJasperDTO.getId(), ModelJasper.class, connection);

		if (StringUtils.isNull(this.modelJasperDTO.getId()) && StringUtils.isNull(modelJasperDTO.getFileUpload())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelJasperDTO.class, "fileUpload"));
		}

		if (StringUtils.isNull(this.modelJasperDTO.getId())
				&& StringUtils.isNull(modelJasperDTO.getFileUpload().getId())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelJasperDTO.class, "fileUpload"));
		}

		DatabaseParameter databaseParameterUnique = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("modelId", this.modelJasperDTO.getModelId().toString()),
						DatabaseFilter.valueOf("name", this.modelJasperDTO.getName()) });
		if (StringUtils.notNull(modelJasperDTO.getId()))
			databaseParameterUnique.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelJasperDTO.getId().toString()));

		if (databaseService.exists(databaseParameterUnique, ModelJasperDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(ModelJasperDTO.class, "name"),
					modelJasperDTO.getName());
		customModelMapper().map(modelJasperDTO, modelJasper);
		if (StringUtils.notNull(modelJasperDTO.getFileUpload().getId())) {
			FileUpload fileUpload = databaseService.findById(modelJasperDTO.getFileUpload().getId(), FileUpload.class,
					connection);

			File file = UpdateObject.createUploadFile(fileUpload, commonService);

			JasperReport jasperReport = null;
			try {
				jasperReport = (JasperReport) JRLoader.loadObject(file);
			} catch (JRException | ClassCastException e) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidJasperFile",
						modelJasperDTO.getFileUpload().getFileName());
			}

			if (Arrays.asList(jasperReport.getParameters()).stream().filter(a -> !a.isSystemDefined())
					.filter(a -> a.getName().equals(SqlQueryWriterServiceImpl.defaultIdColumn))
					.filter(a -> a.getValueClass().equals(Object.class)).count() == 0)
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "missingReportParameter",
						SqlQueryWriterServiceImpl.defaultIdColumn);
			ModelFile modelFile = createModelFile(modelJasper.getFile() == null ? null : modelJasper.getFile().getId(),
					file, modelJasperDTO.getFileUpload().getFileName(), fileUpload.getPath(), connection);
			modelJasper.setFile(modelFile);

		}

		modelJasper = databaseService.save(modelJasper, connection);

		return this.modelMapper.map(modelJasper, ModelJasperDTO.class);
	}

	public static ModelFile createModelFile(UUID id, File file, String fileName, String path, Connection connection) {
		ModelFile modelFile = new ModelFile();
		modelFile.setFileName(fileName);
		modelFile.setId(id);
		modelFile.setMimeType(UpdateObject.findMimeType(file));
		modelFile.setPath(path);
		modelFile = databaseService.save(modelFile, connection);
		return modelFile;
	}

	private ModelMapper customModelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.addMappings(new PropertyMap<ModelJasperDTO, ModelJasper>() {

			@Override
			protected void configure() {
				skip(destination.getFile());

			}
		});
		return modelMapper;
	}

}
