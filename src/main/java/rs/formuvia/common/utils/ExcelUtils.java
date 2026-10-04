package rs.formuvia.common.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class ExcelUtils {
	private static final String EXCEL_DATE_FORMAT = "excel.date.format";
	private static final String EXCEL_DATE_TIME_FORMAT = "excel.date-time.format";
	private static final String EXCEL_TIME_FORMAT = "excel.time.format";
	private static final String DECIMAL_TO_REPLACE = ".00";
	private static final String DECIMAL_FORMAT = "#,##0.00";
	private static final String INTEGER_FORMAT = "0";
	private static final String STRING_FORMAT = "@";
	public static final String COMMON_YES = "common.yes";
	public static final String COMMON_NO = "common.no";

	public static XSSFCell createTableTitleCell(XSSFWorkbook workbook, XSSFRow row, int index, String text) {
		XSSFCell cell = row.createCell(index);
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		xssfCellStyle.setWrapText(true);
		cell.setCellValue(text);
		xssfCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
		xssfCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font font = xssfCellStyle.getFont();
		font.setBold(true);
		xssfCellStyle.setFont(font);
		cell.setCellStyle(xssfCellStyle);
		return cell;
	}

	public static XSSFCell createTableTitleRedCell(XSSFWorkbook workbook, XSSFRow row, int index, String text) {
		XSSFCell xssfCell = createTableTitleCell(workbook, row, index, text);
		XSSFCellStyle xssfCellStyle = xssfCell.getCellStyle();
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setFillForegroundColor(IndexedColors.RED1.index);
		return xssfCell;
	}

	public static XSSFCellStyle dateExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat()
				.getFormat(StaticData.appProperties.getProperty(EXCEL_DATE_FORMAT)));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setWrapText(true);
		return xssfCellStyle;
	}

	public static XSSFCellStyle dateTimeExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat()
				.getFormat(StaticData.appProperties.getProperty(EXCEL_DATE_TIME_FORMAT)));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle timeExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat()
				.getFormat(StaticData.appProperties.getProperty(EXCEL_TIME_FORMAT)));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle decimalExcelStyle(XSSFWorkbook workbook, Integer numberOfDecimal) {

		String[] decimalNumber = new String[numberOfDecimal];
		for (int i = 0; i < numberOfDecimal; i++) {
			decimalNumber[i] = "0";
		}
		String textToReplace = "";
		if (numberOfDecimal > 0)
			textToReplace = "." + Arrays.asList(decimalNumber).stream().collect(Collectors.joining());
		String decimalFormat = DECIMAL_FORMAT.replace(DECIMAL_TO_REPLACE, textToReplace);

		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(decimalFormat));
		xssfCellStyle.setAlignment(HorizontalAlignment.RIGHT);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle integerExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(INTEGER_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.RIGHT);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle stringExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(STRING_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.LEFT);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle booleanExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(STRING_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setWrapText(true);
		xssfCellStyle.setLocked(false);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCell createCell(XSSFWorkbook workbook, XSSFRow row, int index, ColumnType columnType,
			ResourceBundleService resourceBundleService, Object value, List<ComboboxDTO> listOfValues,
			Integer numberOfDecimal) {
		XSSFCell xssfCell = row.createCell(index);
		if (StringUtils.isNull(value))
			return xssfCell;

		switch (columnType) {
		case BIGDECIMAL:
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			xssfCell.setCellValue(bigDecimal.doubleValue());
			xssfCell.setCellStyle(decimalExcelStyle(workbook, numberOfDecimal));
			break;
		case BOOLEAN:
			Boolean booleanValue = Boolean.valueOf(value.toString());
			String cellValue = null;
			if (booleanValue)
				cellValue = resourceBundleService.getText(COMMON_YES);
			else
				cellValue = resourceBundleService.getText(COMMON_NO);
			xssfCell.setCellValue(cellValue);
			xssfCell.setCellStyle(booleanExcelStyle(workbook));
			break;
		case INTEGER, LONG:
			Long longValue = Long.valueOf(value.toString());
			xssfCell.setCellValue(longValue.longValue());
			xssfCell.setCellStyle(integerExcelStyle(workbook));
			break;
		case LOCALDATE:
			xssfCell.setCellValue(LocalDate.parse(value.toString()));
			xssfCell.setCellStyle(dateExcelStyle(workbook));
			break;
		case LOCALDATETIME:
			xssfCell.setCellValue(LocalDateTime.parse(value.toString()));
			xssfCell.setCellStyle(dateTimeExcelStyle(workbook));
			break;
		case LOCALTIME: {
			LocalTime localTime = LocalTime.parse(value.toString());
			xssfCell.setCellValue(localTime.toSecondOfDay() / 86400d);
			xssfCell.setCellStyle(timeExcelStyle(workbook));
			break;
		}
		case STRING, UUID, FILE:
			xssfCell.setCellStyle(stringExcelStyle(workbook));
			if (StringUtils.isNull(listOfValues) || listOfValues.isEmpty()) {
				xssfCell.setCellValue(value.toString());
			} else {
				ComboboxDTO box = listOfValues.stream().filter(a -> a.getValue().toString().equals(value.toString()))
						.findFirst().orElse(null);
				if (StringUtils.notNull(box))
					xssfCell.setCellValue(box.getOption());
			}

			break;

		}

		return xssfCell;
	}

	public static XSSFCell createCellLocked(XSSFWorkbook workbook, XSSFRow row, int index, ColumnType columnType,
			ResourceBundleService resourceBundleService, Object value, List<ComboboxDTO> listOfValues,
			Integer numberOfDecimal) {
		XSSFCell cell = createCell(workbook, row, index, columnType, resourceBundleService, value, listOfValues,
				numberOfDecimal);
		XSSFCellStyle cellStyle = cell.getCellStyle();
		cellStyle.setLocked(true);
		cell.setCellStyle(cellStyle);
		return cell;
	}

	public static XSSFCell createCell(XSSFWorkbook workbook, XSSFRow row, int index, ColumnType columnType,
			Object value, List<ComboboxDTO> listOfValues, Integer numberOfDecimal) {
		ResourceBundleService resourceBundleService = new ResourceBundleServiceImpl();
		return createCell(workbook, row, index, columnType, resourceBundleService, value, listOfValues,
				numberOfDecimal);
	}

}
