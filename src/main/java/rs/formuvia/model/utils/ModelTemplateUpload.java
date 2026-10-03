package rs.formuvia.model.utils;

import java.io.File;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.entity.FileUpload;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.ExcelImportException;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class ModelTemplateUpload implements ExecuteQuery<Void> {

	private final HttpServletRequest httpServletRequest;
	private final UUID fileUploadDTOId;
	private final UUID modelId;
	private final UUID parentId;
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public ModelTemplateUpload(HttpServletRequest httpServletRequest, UUID fileUploadDTOId, UUID modelId,
			UUID parentId) {
		this.httpServletRequest = httpServletRequest;
		this.fileUploadDTOId = fileUploadDTOId;
		this.modelId = modelId;
		this.parentId = parentId;
		this.commonService = new CommonServiceImpl(httpServletRequest);
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getAddRoleCode());

		FileUpload fileUpload = databaseService.findById(fileUploadDTOId, FileUpload.class, connection);

		File file = UpdateObject.createUploadFile(fileUpload, connection, commonService);
		String mimeType = UpdateObject.findMimeType(file);
		if (!mimeType.equals(ExportTableServiceImpl.EXCEL_CONTENT_TYPE)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongFormat", mimeType);
		}

		XSSFWorkbook wb = new XSSFWorkbook(file);

		LinkedHashMap<String, Object> booleanMap = booleanMap(wb);

		List<ModelColumnDTO> columns = createList(wb, this.modelId);

		Boolean hasParent = UpdateObject.tableHasParent(modelDTO);
		XSSFSheet importSheet = wb.getSheet(ModelTemplateDownload.IMPORT_SHEET_NAME);
		for (int i = 1; i <= importSheet.getLastRowNum(); i++) {

			try {
				XSSFRow xssfRow = importSheet.getRow(i);
				if (!checkRowExists(xssfRow)) {
					continue;
				}
				LinkedHashMap<String, Object> object = new LinkedHashMap<>();

				if (hasParent)
					object.put(UpdateModel.PARENT_COLUMN_NAME, this.parentId);

				int columnIndex = -1;

				for (ModelColumnDTO modelColumnDTO : columns) {
					columnIndex++;

					ColumnType columnType = modelColumnDTO.getColumnType();

					if (columnType.equals(ColumnType.BOOLEAN) || columnType.equals(ColumnType.UUID)
							|| hasCodebookValue(modelColumnDTO))
						columnType = ColumnType.STRING;

					Object value = getCellValue(xssfRow.getCell(columnIndex), columnType);
					if (StringUtils.isNull(value)) {
						continue;
					}

					if (modelColumnDTO.getColumnType().equals(ColumnType.BOOLEAN)) {
						if (StringUtils.isNull(booleanMap.get(value))) {
							throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noBooleanValueFound", value);
						}
						value = booleanMap.get(value);
					}

					if (hasCodebookValue(modelColumnDTO)) {
						String sheetName = ModelTemplateDownload.getCodebookSheetName(modelColumnDTO.getId());
						LinkedHashMap<String, Object> map = createWorkbookMap(wb.getSheet(sheetName),
								modelColumnDTO.getColumnType());
						if (map.get(value) == null) {
							throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noValueFound", value);
						}

						value = map.get(value);

					}

					object.put(modelColumnDTO.getCode(), value);
				}

				UpdateObject updateObject = new UpdateObject(httpServletRequest, object, modelId);
				databaseService.executeQuery(updateObject, connection);

			} catch (Exception e) {
				wb.close();
				throw new ExcelImportException(i + 1, e);
			}

		}
		wb.close();

		return null;
	}

	public static Boolean hasCodebookValue(ModelColumnDTO modelColumnDTO) {
		return (StringUtils.hasText(modelColumnDTO.getListOfValuesSql())
				|| StringUtils.notNull(modelColumnDTO.getCodebookId()));
	}

	private List<ModelColumnDTO> createList(XSSFWorkbook wb, UUID modelId) {
		List<ModelColumnDTO> list = new ArrayList<>();
		LinkedHashMap<String, Object> columnMap = columnMap(wb);
		List<ModelColumnDTO> listColumns = ModelTemplateDownload.findColumnsForExcel(modelId);
		Iterator<String> iterator = columnMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			ModelColumnDTO findColumn = listColumns.stream().filter(a -> a.getId().equals(UUID.fromString(key)))
					.findFirst()
					.orElseThrow(() -> new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noColumnFound", key));
			list.add(findColumn);
		}
		return list;
	}

	private LinkedHashMap<String, Object> booleanMap(XSSFWorkbook xssfWorkbook) {

		XSSFSheet xssfSheet = xssfWorkbook.getSheet(ModelTemplateDownload.BOOLEAN_SHEET_NAME);
		return createWorkbookMap(xssfSheet, ColumnType.STRING);

	}

	private LinkedHashMap<String, Object> columnMap(XSSFWorkbook xssfWorkbook) {

		XSSFSheet xssfSheet = xssfWorkbook.getSheet(ModelTemplateDownload.COLUMNS_SHEET_NAME);
		return createWorkbookMap(xssfSheet, ColumnType.INTEGER);

	}

	private LinkedHashMap<String, Object> createWorkbookMap(XSSFSheet sheet, ColumnType columnTypeFirstColumn) {
		if (sheet == null) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noWorkbookSheetFound", null);
		}
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();

		for (int i = 0; i <= sheet.getLastRowNum(); i++) {
			XSSFRow xssfRow = sheet.getRow(i);
			if (!checkRowExists(xssfRow)) {
				continue;
			}
			Object value = getCellValue(xssfRow.getCell(0), columnTypeFirstColumn);
			String option = (String) getCellValue(xssfRow.getCell(1), ColumnType.STRING);

			if (!StringUtils.isNull(value)) {
				map.put(option, value);
			}

		}

		return map;
	}

	private Boolean checkRowExists(XSSFRow xssfRow) {
		if (xssfRow == null) {
			return false;
		}
		for (int i = 0; i < xssfRow.getLastCellNum(); i++) {
			if (findCellValue(xssfRow.getCell(i)) != null)
				return true;
		}
		return false;
	}

	private Object getCellValue(XSSFCell cell, ColumnType columnType) {
		Object value = findCellValue(cell);
		if (StringUtils.isNull(value)) {
			return null;
		}

		switch (columnType) {
		case BIGDECIMAL:
			Number number = (Number) value;
			return BigDecimal.valueOf(number.doubleValue());
		case BOOLEAN:
			return (Boolean) value;
		case FILE:
			return null;
		case INTEGER:
			number = (Number) value;
			return number.intValue();
		case LOCALDATE:
			LocalDateTime localDateTime = (LocalDateTime) value;
			return localDateTime.toLocalDate();
		case LOCALDATETIME:
			return (LocalDateTime) value;
		case LOCALTIME:
			localDateTime = (LocalDateTime) value;
			return localDateTime.toLocalTime();
		case LONG:
			number = (Number) value;
			return number.longValue();
		case STRING:
			return value.toString();
		case UUID:
			return UUID.fromString(value.toString());

		}

		return null;
	}

	private Object findCellValue(XSSFCell cell) {
		if (cell == null) {
			return null;
		}

		switch (cell.getCellType()) {
		case BLANK:
			return null;
		case BOOLEAN:
			return cell.getBooleanCellValue();
		case ERROR:
			return cell.getErrorCellString();
		case FORMULA:
			return cell.getCellFormula();
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getLocalDateTimeCellValue();
			} else {
				return cell.getNumericCellValue();
			}

		case STRING:
			return cell.getStringCellValue();
		case _NONE:
			return null;

		}

		return null;
	}

}
