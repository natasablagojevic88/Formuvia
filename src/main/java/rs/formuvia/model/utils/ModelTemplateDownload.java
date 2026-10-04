package rs.formuvia.model.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidation.ErrorStyle;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.common.utils.ExcelUtils;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.ParentListOfValues;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class ModelTemplateDownload implements ExecuteQuery<Response> {
	private HttpServletRequest httpServletRequest;
	private UUID modelId;
	public static String IMPORT_SHEET_NAME = "import";
	private final Integer EXCEL_MAXIMUM_ROW_NUMBER = 1048576;
	private final String MINIMUM_BIG_NUMBER = "-999999999999999";
	private final String MAXIMUM_BIG_NUMBER = "999999999999999";
	public static String BOOLEAN_SHEET_NAME = "BOOLEAN_SHEET";
	public static String COLUMNS_SHEET_NAME = "COLUMNS_SHEET";
	private final String NUMBER_ERROR = "onlyNumberAllowed";
	private final String LIST_OF_VALUES_ERROR = "noDataInList";
	private final String DATE_ERROR = "onlyDateAllowed";
	private final String TIME_ERROR = "onlyTimeAllowed";
	private final String MINIMUM_DATE = "1900-01-01";
	private final String MAXIMUM_DATE = "2999-12-31";
	private final String MINIMUM_TIME = "00:00";
	private final String MAXIMUM_TIME = "23:59";

	private ResourceBundleService resourseBundleService;
	private CommonService commonService;

	public ModelTemplateDownload(HttpServletRequest httpServletRequest, UUID modelId) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.resourseBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Response execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());
		XSSFWorkbook wb = new XSSFWorkbook();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XSSFSheet xssfSheet = wb.createSheet(IMPORT_SHEET_NAME);

		addBooleanSheet(wb, resourseBundleService);

		List<ModelColumnDTO> columns = findColumnsForExcel(this.modelId);

		List<ComboboxDTO> columnIndexes = columns.stream().map(a -> {
			return new ComboboxDTO(columns.indexOf(a), a.getId().toString());
		}).toList();
		createCodebookSheet(wb, COLUMNS_SHEET_NAME, columnIndexes, resourseBundleService, ColumnType.INTEGER);
		int rowIndex = 0;
		int columnIndex = -1;
		XSSFRow xssfRow = xssfSheet.createRow(rowIndex);
		for (ModelColumnDTO modelColumnDTO : columns) {
			columnIndex++;
			if (modelColumnDTO.getNullable())
				ExcelUtils.createTableTitleCell(wb, xssfRow, columnIndex,
						this.resourseBundleService.getText(modelColumnDTO.getName()));
			else
				ExcelUtils.createTableTitleRedCell(wb, xssfRow, columnIndex,
						this.resourseBundleService.getText(modelColumnDTO.getName()));
			ExportTableServiceImpl.formatColumnType(modelColumnDTO.getColumnType(), xssfSheet, columnIndex,
					modelColumnDTO.getLength());
			if (ModelTemplateUpload.hasCodebookValue(modelColumnDTO))
				ExportTableServiceImpl.formatColumnType(ColumnType.STRING, xssfSheet, columnIndex,
						modelColumnDTO.getLength());
		}

		columnIndex = -1;
		for (ModelColumnDTO columnDTO : columns) {
			columnIndex++;
			xssfSheet.autoSizeColumn(columnIndex, true);

			if (xssfSheet.getColumnWidth(columnIndex) < minWidth(columnDTO.getColumnType()))
				xssfSheet.setColumnWidth(columnIndex, minWidth(columnDTO.getColumnType()));

			cellValidation(columnDTO, xssfSheet, columnIndex, resourseBundleService, columnDTO.getNullable(),
					connection);
		}

		xssfSheet.protectSheet(UUID.randomUUID().toString());
		xssfSheet.lockFormatCells(false);
		xssfSheet.lockFormatColumns(false);

		try {

			wb.write(baos);

			wb.close();
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}

		return ExportTableServiceImpl.createExcelResponse(baos.toByteArray(), modelDTO.getCode() + ".xlsx",
				ExportTableServiceImpl.EXCEL_CONTENT_TYPE);
	}

	public static List<ModelColumnDTO> findColumnsForExcel(UUID modelId) {
		return StaticData.modelColumns.stream().filter(a -> a.getEditable()).filter(a -> a.getModelId().equals(modelId))
				.filter(a -> !a.getColumnType().equals(ColumnType.FILE))
				.sorted(Comparator.comparing(ModelColumnDTO::getRowIndex)
						.thenComparing(Comparator.comparing(ModelColumnDTO::getColumnIndex)))
				.collect(Collectors.toList());
	}

	private void addBooleanSheet(XSSFWorkbook wb, ResourceBundleService resourceBundleService) {
		List<ComboboxDTO> list = new ArrayList<>();
		list.add(new ComboboxDTO(String.valueOf(true), resourceBundleService.getText(ExcelUtils.COMMON_YES)));
		list.add(new ComboboxDTO(String.valueOf(false), resourceBundleService.getText(ExcelUtils.COMMON_NO)));

		createCodebookSheet(wb, BOOLEAN_SHEET_NAME, list, resourceBundleService, ColumnType.STRING);

	}

	private void createCodebookSheet(XSSFWorkbook wb, String sheetName, List<ComboboxDTO> list,
			ResourceBundleService resourceBundleService, ColumnType columnType) {
		XSSFSheet sheet = wb.createSheet(sheetName);
		int index = -1;
		for (ComboboxDTO item : list) {
			index++;
			XSSFRow row = sheet.createRow(index);
			ExcelUtils.createCellLocked(wb, row, 0, columnType, resourceBundleService, item.getValue(), null, null);
			ExcelUtils.createCellLocked(wb, row, 1, ColumnType.STRING, resourceBundleService, item.getOption(), null,
					null);
		}
		sheet.protectSheet(UUID.randomUUID().toString());
		wb.setSheetHidden(wb.getSheetIndex(sheet), true);
	}

	private Integer minWidth(ColumnType type) {
		Integer width = 0;
		switch (type) {
		case LOCALTIME -> width = 8;
		case BOOLEAN -> width = 10;
		case LOCALDATE, INTEGER -> width = 12;
		case BIGDECIMAL -> width = 14;
		case LOCALDATETIME -> width = 18;
		case LONG -> width = 20;
		case FILE, STRING, UUID -> width = 30;
		}
		;

		return 256 * width;

	}

	private void cellValidation(ModelColumnDTO modelColumnDTO, XSSFSheet xssfSheet, int columnIndex,
			ResourceBundleService resourceBundleService, Boolean nullable, Connection connection) {
		DataValidationHelper helper = xssfSheet.getDataValidationHelper();
		CellRangeAddressList range = new CellRangeAddressList(1, EXCEL_MAXIMUM_ROW_NUMBER - 1, columnIndex,
				columnIndex);

		DataValidationConstraint constraint = null;
		String errorMessage = null;
		if (StringUtils.hasText(modelColumnDTO.getListOfValuesSql())) {
			List<ComboboxDTO> listOfValues = CreateForm.createListOfValues(modelColumnDTO.getListOfValuesSql(),
					connection, resourceBundleService);
			createCodebookSheet(xssfSheet.getWorkbook(), getCodebookSheetName(modelColumnDTO.getId()), listOfValues,
					resourceBundleService, modelColumnDTO.getColumnType());
			constraint = listOfValues(xssfSheet.getWorkbook(), getCodebookSheetName(modelColumnDTO.getId()));
			errorMessage = LIST_OF_VALUES_ERROR;
		} else {
			switch (modelColumnDTO.getColumnType()) {
			case BIGDECIMAL, LONG:
				constraint = helper.createDecimalConstraint(OperatorType.BETWEEN, MINIMUM_BIG_NUMBER,
						MAXIMUM_BIG_NUMBER);
				errorMessage = NUMBER_ERROR;
				break;
			case BOOLEAN:
				constraint = listOfValues(xssfSheet.getWorkbook(), BOOLEAN_SHEET_NAME);
				errorMessage = LIST_OF_VALUES_ERROR;
				break;
			case INTEGER:
				constraint = helper.createIntegerConstraint(OperatorType.BETWEEN,
						String.valueOf(-1 * Integer.MAX_VALUE), String.valueOf(Integer.MAX_VALUE));
				errorMessage = NUMBER_ERROR;
				break;
			case LOCALDATE, LOCALDATETIME:
				constraint = helper.createDateConstraint(OperatorType.BETWEEN,
						String.valueOf(DateUtil.getExcelDate(LocalDate.parse(MINIMUM_DATE))),
						String.valueOf(DateUtil.getExcelDate(LocalDate.parse(MAXIMUM_DATE))), null);
				errorMessage = DATE_ERROR;
				break;
			case LOCALTIME:
				constraint = helper.createTimeConstraint(OperatorType.BETWEEN,
						String.valueOf(DateUtil.convertTime(MINIMUM_TIME)),
						String.valueOf(DateUtil.convertTime(MAXIMUM_TIME)));
				errorMessage = TIME_ERROR;
				break;
			case UUID:
				if (StringUtils.notNull(modelColumnDTO.getCodebookId())) {
					createCodebookValidation(modelColumnDTO, this.resourseBundleService, xssfSheet.getWorkbook());
					constraint = listOfValues(xssfSheet.getWorkbook(), getCodebookSheetName(modelColumnDTO.getId()));
					errorMessage = LIST_OF_VALUES_ERROR;
				}
				break;
			case STRING, FILE:
				break;

			}
		}

		if (constraint == null)
			return;

		DataValidation validation = helper.createValidation(constraint, range);
		validation.setEmptyCellAllowed(nullable);
		validation.setShowErrorBox(true);
		validation.setErrorStyle(ErrorStyle.STOP);
		validation.createErrorBox(resourseBundleService.getText(modelColumnDTO.getName()),
				resourseBundleService.getText(errorMessage));
		xssfSheet.addValidationData(validation);
	}

	private DataValidationConstraint listOfValues(XSSFWorkbook xssfWorkbook, String sheetName) {
		XSSFSheet xssfSheet = xssfWorkbook.getSheet(sheetName);
		DataValidationHelper helper = xssfSheet.getDataValidationHelper();
		DataValidationConstraint constraint = helper
				.createFormulaListConstraint("'" + sheetName + "'!$B$1:$B$" + (xssfSheet.getLastRowNum() + 1));
		return constraint;

	}

	private void createCodebookValidation(ModelColumnDTO column, ResourceBundleService resourceBundleService,
			XSSFWorkbook xssfWorkbook) {
		List<ComboboxDTO> list = StaticData.modelCodebook.get(column.getCodebookId());
		if (StringUtils.isNull(list)) {
			return;
		}
		List<ParentListOfValues> parentListOfValues = new ArrayList<>();
		CreateModelTable.createParentList(parentListOfValues, column.getCodebookId(), resourceBundleService, null,
				null);

		List<ComboboxDTO> listForValidation = new ArrayList<>();
		for (ComboboxDTO item : list) {
			List<String> values = new ArrayList<>();
			createCodebookName(item, values, parentListOfValues, column.getCodebookId());
			List<String> valuesReversed = new ArrayList<>();
			for (int i = values.size() - 1; i >= 0; i--) {
				valuesReversed.add(values.get(i));
			}
			listForValidation
					.add(new ComboboxDTO(item.getValue(), valuesReversed.stream().collect(Collectors.joining(" - "))));
		}
		listForValidation = listForValidation.stream().sorted(Comparator.comparing(ComboboxDTO::getOption))
				.collect(Collectors.toList());

		createCodebookSheet(xssfWorkbook, getCodebookSheetName(column.getId()), listForValidation,
				resourceBundleService, ColumnType.UUID);

	}

	private void createCodebookName(ComboboxDTO item, List<String> values, List<ParentListOfValues> parentListOfValues,
			UUID chidModelId) {
		values.add(item.getOption());
		if (!StringUtils.isNull(item.getParent())) {
			ParentListOfValues parentModel = parentListOfValues.stream().filter(a -> a.getChild().equals(chidModelId))
					.findFirst().orElse(null);

			if (StringUtils.isNull(parentModel))
				return;

			List<ComboboxDTO> comboboxDTOs = StaticData.modelCodebook.get(parentModel.getParent());
			if (StringUtils.isNull(comboboxDTOs)) {
				return;
			}
			ComboboxDTO parentItem = comboboxDTOs.stream().filter(a -> a.getValue().equals(item.getParent()))
					.findFirst().orElse(null);
			if (StringUtils.isNull(parentItem))
				return;
			createCodebookName(parentItem, values, parentListOfValues, item.getParent());
		}
	}

	public static String getCodebookSheetName(UUID uuid) {
		return uuid.toString().replaceAll("-", "").substring(1);
	}

}
