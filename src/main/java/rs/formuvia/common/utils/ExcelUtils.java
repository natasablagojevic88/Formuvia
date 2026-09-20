package rs.formuvia.common.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class ExcelUtils {
	private static final String EXCEL_DATE_FORMAT = "excel.date.format";
	private static final String EXCEL_DATE_TIME_FORMAT = "excel.date-time.format";
	private static final String DECIMAL_FORMAT = "#,##0.00";
	private static final String INTEGER_FORMAT = "0";
	private static final String STRING_FORMAT = "@";
	private static final String COMMON_YES = "common.yes";
	private static final String COMMON_NO = "common.no";

	public static XSSFCell createTableTitleCell(XSSFWorkbook workbook, XSSFRow row, int index, String text) {
		XSSFCell cell = row.createCell(index);
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		cell.setCellValue(text);
		xssfCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
		xssfCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font font = xssfCellStyle.getFont();
		font.setBold(true);
		xssfCellStyle.setFont(font);
		cell.setCellStyle(xssfCellStyle);
		return cell;
	}

	public static XSSFCellStyle dateExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat()
				.getFormat(StaticData.appProperties.getProperty(EXCEL_DATE_FORMAT)));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle dateTimeExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat()
				.getFormat(StaticData.appProperties.getProperty(EXCEL_DATE_TIME_FORMAT)));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle decimalExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(DECIMAL_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.RIGHT);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle integerExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(INTEGER_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.RIGHT);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle stringExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(STRING_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.LEFT);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCellStyle booleanExcelStyle(XSSFWorkbook workbook) {
		XSSFCellStyle xssfCellStyle = workbook.createCellStyle();
		XSSFCreationHelper xssfCreationHelper = workbook.getCreationHelper();
		xssfCellStyle.setDataFormat(xssfCreationHelper.createDataFormat().getFormat(STRING_FORMAT));
		xssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
		xssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return xssfCellStyle;
	}

	public static XSSFCell createCell(XSSFWorkbook workbook, XSSFRow row, int index, ColumnType columnType,
			ResourceBundleService resourceBundleService, Object value) {
		XSSFCell xssfCell = row.createCell(index);
		if (StringUtils.isNull(value))
			return xssfCell;

		switch (columnType) {
		case BIGDECIMAL:
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			xssfCell.setCellValue(bigDecimal.doubleValue());
			break;
		case BOOLEAN:
			Boolean booleanValue = Boolean.valueOf(value.toString());
			String cellValue = null;
			if (booleanValue)
				cellValue = resourceBundleService.getText(COMMON_YES);
			else
				cellValue = resourceBundleService.getText(COMMON_NO);
			xssfCell.setCellValue(cellValue);
			break;
		case INTEGER, LONG:
			Long longValue = Long.valueOf(value.toString());
			xssfCell.setCellValue(longValue.longValue());
			break;
		case LOCALDATE:
			xssfCell.setCellValue(LocalDate.parse(value.toString()));
			break;
		case LOCALDATETIME:
			xssfCell.setCellValue(LocalDateTime.parse(value.toString()));
			break;
		case STRING, UUID:
			xssfCell.setCellValue(value.toString());
			break;

		}

		return xssfCell;
	}

	public static XSSFCell createCell(XSSFWorkbook workbook, XSSFRow row, int index, ColumnType columnType,
			Object value) {
		ResourceBundleService resourceBundleService = new ResourceBundleServiceImpl();
		return createCell(workbook, row, index, columnType, resourceBundleService, value);
	}

}
