package rs.formuvia.common.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.ExportTableService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.utils.ExcelUtils;
import rs.formuvia.database.utils.DatabaseColumn;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.CustomContainerRequestFilter;

@Service
public class ExportTableServiceImpl implements ExportTableService {

	private final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private final String CONTENT_DISPOSITION = "Content-Disposition";
	private final String EXPORT_FILE_NAME = "export.xlsx";
	private final String CONTENT_DISPOTION_TEXT = "\"attachment; filename*=UTF-8''";
	private final String SHEET_NAME = "export";
	private final String EXPORT_HEADER = "Access-Control-Expose-Headers";

	@Inject
	private ResourceBundleService resourceBundleService;

	@SuppressWarnings("unchecked")
	@Override
	public Response getExport(DatabaseTable<?> databaseTable) {
		XSSFWorkbook wb = new XSSFWorkbook();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		XSSFSheet sheet = wb.createSheet(SHEET_NAME);
		sheet.setDisplayGridlines(false);
		XSSFRow row = sheet.createRow(0);
		ExcelUtils.createTableTitleCell(wb, row, 0, databaseTable.getName());

		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, databaseTable.getColumn().size() - 1));

		row = sheet.createRow(1);

		int index = -1;
		for (DatabaseColumn column : databaseTable.getColumn()) {
			index++;
			ExcelUtils.createTableTitleCell(wb, row, index, column.getDescription());
			switch (column.getColumnType()) {
			case BIGDECIMAL:
				sheet.setDefaultColumnStyle(index, ExcelUtils.decimalExcelStyle(wb));
				break;
			case INTEGER, LONG:
				sheet.setDefaultColumnStyle(index, ExcelUtils.integerExcelStyle(wb));
				break;
			case LOCALDATE:
				sheet.setDefaultColumnStyle(index, ExcelUtils.dateExcelStyle(wb));
				break;
			case LOCALDATETIME:
				sheet.setDefaultColumnStyle(index, ExcelUtils.dateTimeExcelStyle(wb));
				break;
			case BOOLEAN: {
				sheet.setDefaultColumnStyle(index, ExcelUtils.booleanExcelStyle(wb));
				break;
			}
			case STRING, UUID:
				sheet.setDefaultColumnStyle(index, ExcelUtils.stringExcelStyle(wb));
				break;

			}

		}
		index = 1;
		for (Object item : databaseTable.getList()) {
			index++;
			Map<String, Object> map = (Map<String, Object>) item;
			row = sheet.createRow(index);
			int rowIndex = -1;
			for (DatabaseColumn column : databaseTable.getColumn()) {
				rowIndex++;
				ExcelUtils.createCell(wb, row, rowIndex, column.getColumnType(), resourceBundleService,
						map.get(column.getFieldName()));
			}

		}

		for (int i = 0; i < databaseTable.getColumn().size(); i++) {
			sheet.autoSizeColumn(i, true);

		}

		try {

			wb.write(baos);

			wb.close();
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}

		return createExcelResponse(baos.toByteArray(), EXPORT_FILE_NAME);
	}

	private Response createExcelResponse(byte[] bytes, String filename) {
		String encodedName = null;
		try {
			encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.displayName()).replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new WebApplicationException(e);
		}
		Response response = Response.status(HttpURLConnection.HTTP_OK)
				.header(CustomContainerRequestFilter.CONTENT_TYPE, EXCEL_CONTENT_TYPE)
				.header(EXPORT_HEADER, CONTENT_DISPOSITION)
				.header(CONTENT_DISPOSITION, CONTENT_DISPOTION_TEXT + encodedName).entity(bytes).build();
		return response;
	}

}
