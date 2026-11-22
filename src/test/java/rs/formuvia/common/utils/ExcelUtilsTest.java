package rs.formuvia.common.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.utils.StaticData;

/**
 * The export of a table and of a report to Excel (ExportTableServiceImpl, ReportPreviewServiceImpl) writes every
 * value through ExcelUtils.createCell: a number must stay a number and a date a date, so that Excel can sum and
 * sort them. The format of a column (decimals, date) is set once per column with
 * ExportTableServiceImpl.formatColumnType, and every new cell of that column takes it.
 */
class ExcelUtilsTest {

	/** Translates only the two texts a yes/no column needs. */
	private static final ResourceBundleService TEXTS = key -> switch (key) {
	case ExcelUtils.COMMON_YES -> "Yes";
	case ExcelUtils.COMMON_NO -> "No";
	default -> key;
	};

	private XSSFWorkbook workbook;
	private XSSFRow row;

	@BeforeAll
	static void formats() {
		// the same keys as in application.properties
		StaticData.appProperties.setProperty("excel.date.format", "dd.MM.yyyy");
		StaticData.appProperties.setProperty("excel.date-time.format", "dd.MM.yyyy HH:mm");
		StaticData.appProperties.setProperty("excel.time.format", "HH:mm");
	}

	@BeforeEach
	void open() {
		workbook = new XSSFWorkbook();
		row = workbook.createSheet("test").createRow(0);
	}

	@AfterEach
	void close() throws IOException {
		workbook.close();
	}

	private XSSFCell cell(ColumnType type, Object value) {
		return ExcelUtils.createCell(workbook, row, 0, type, TEXTS, value, null, 2);
	}

	@Test
	void emptyValueGivesAnEmptyCell() {
		assertEquals(CellType.BLANK, cell(ColumnType.STRING, null).getCellType());
		assertEquals(CellType.BLANK, cell(ColumnType.INTEGER, " ").getCellType());
	}

	@Test
	void numbersStayNumbers() {
		assertEquals(1234.56, cell(ColumnType.BIGDECIMAL, "1234.56").getNumericCellValue());
		assertEquals(42, cell(ColumnType.INTEGER, 42).getNumericCellValue());
		assertEquals(9_000_000_000d, cell(ColumnType.LONG, "9000000000").getNumericCellValue());
	}

	/** A cell of a column formatted the way both exports do it. */
	private XSSFCell formattedCell(int index, ColumnType type, Object value, Integer decimals) {
		ExportTableServiceImpl.formatColumnType(type, row.getSheet(), index, decimals);
		return ExcelUtils.createCell(workbook, row, index, type, TEXTS, value, null, decimals);
	}

	@Test
	void decimalsAreShownWithTheNumberOfDecimalsOfTheColumn() {
		XSSFCell cell = formattedCell(0, ColumnType.BIGDECIMAL, "3.14159", 3);
		assertTrue(cell.getCellStyle().getDataFormatString().endsWith("0.000"), cell.getCellStyle().getDataFormatString());
	}

	@Test
	void columnsOfTheSameTypeShareOneStyle() {
		// a style per cell ends at about 64,000 cells, the limit of Excel
		int styles = workbook.getNumCellStyles();
		XSSFRow second = row.getSheet().createRow(1);
		formattedCell(0, ColumnType.BIGDECIMAL, "1.5", 2);
		ExcelUtils.createCell(workbook, second, 0, ColumnType.BIGDECIMAL, TEXTS, "2.5", null, 2);
		assertEquals(styles + 1, workbook.getNumCellStyles());
		assertEquals(row.getCell(0).getCellStyle().getIndex(), second.getCell(0).getCellStyle().getIndex());
	}

	@Test
	void datesAndTimesAreDatesWithTheFormatFromTheSettings() {
		XSSFCell date = formattedCell(0, ColumnType.LOCALDATE, "2026-11-18", null);
		assertEquals(LocalDateTime.of(2026, 11, 18, 0, 0), date.getLocalDateTimeCellValue());
		assertEquals("dd.MM.yyyy", date.getCellStyle().getDataFormatString());

		XSSFCell dateTime = formattedCell(1, ColumnType.LOCALDATETIME, "2026-11-18T08:30:00", null);
		assertEquals(LocalDateTime.of(2026, 11, 18, 8, 30), dateTime.getLocalDateTimeCellValue());
		assertEquals("dd.MM.yyyy HH:mm", dateTime.getCellStyle().getDataFormatString());

		// a time is a part of the day, as Excel keeps it
		XSSFCell time = formattedCell(2, ColumnType.LOCALTIME, "06:00", null);
		assertEquals(0.25, time.getNumericCellValue(), 1e-9);
		assertEquals("HH:mm", time.getCellStyle().getDataFormatString());
	}

	@Test
	void yesAndNoAreTranslated() {
		assertEquals("Yes", cell(ColumnType.BOOLEAN, true).getStringCellValue());
		assertEquals("No", cell(ColumnType.BOOLEAN, "false").getStringCellValue());
	}

	@Test
	void valueOfAListIsWrittenAsItsOption() {
		UUID id = UUID.randomUUID();
		List<ComboboxDTO> options = List.of(new ComboboxDTO("PAID", "Paid"), new ComboboxDTO(id, "Belgrade"));

		assertEquals("Paid",
				ExcelUtils.createCell(workbook, row, 0, ColumnType.STRING, TEXTS, "PAID", options, null)
						.getStringCellValue());
		assertEquals("Belgrade",
				ExcelUtils.createCell(workbook, row, 1, ColumnType.UUID, TEXTS, id.toString(), options, null)
						.getStringCellValue());
	}

	@Test
	void textIsWrittenAsItIs() {
		assertEquals("Ana Anić", cell(ColumnType.STRING, "Ana Anić").getStringCellValue());
	}

	@Test
	void lockedCellStaysLocked() {
		XSSFCell cell = ExcelUtils.createCellLocked(workbook, row, 0, ColumnType.STRING, TEXTS, "x", null, null);
		assertTrue(cell.getCellStyle().getLocked());
	}

	@Test
	void largeExportFitsIntoOneWorkbook() {
		// an .xlsx holds at most 64000 styles; the styles are made once per column, not per cell
		XSSFSheet sheet = workbook.getSheetAt(0);
		for (int c = 0; c < 8; c++)
			ExportTableServiceImpl.formatColumnType(ColumnType.STRING, sheet, c, null);
		assertDoesNotThrow(() -> {
			for (int r = 1; r <= 10_000; r++) {
				XSSFRow line = sheet.createRow(r);
				for (int c = 0; c < 8; c++)
					ExcelUtils.createCell(workbook, line, c, ColumnType.STRING, TEXTS, "value", null, null);
			}
		});
	}
}
