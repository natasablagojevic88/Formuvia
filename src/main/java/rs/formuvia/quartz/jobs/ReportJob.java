package rs.formuvia.quartz.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.SendMailService;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.common.service.impl.SendMailServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.model.utils.ModelTemplateUpload;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.quartz.entity.JobItem;
import rs.formuvia.quartz.entity.JobLog;
import rs.formuvia.quartz.enums.JobLogStatus;
import rs.formuvia.quartz.enums.JobType;
import rs.formuvia.quartz.utils.JobItemUpdate;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportInfoParameterDTO;
import rs.formuvia.report.dto.ReportResultColumnDTO;
import rs.formuvia.report.dto.ReportResultDTO;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.service.ReportPreviewService;
import rs.formuvia.report.service.impl.ReportPreviewServiceImpl;
import rs.formuvia.utils.CustomDefaultExceptionMapper;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@DisallowConcurrentExecution
public class ReportJob implements Job {

	private DatabaseService databaseService = new DatabaseServiceImpl();

	private UUID jobId;
	private UUID reportId;
	private UUID modelId;

	private final String BACKUP_FOLDER = "/backup";
	private final String ERROR_FOLDER = "/error";

	public ReportJob(UUID jobId, UUID reportId, UUID modelId) {
		this.jobId = jobId;
		this.reportId = reportId;
		this.modelId = modelId;
	}

	public ReportJob() {
	}

	private Logger logger = LogManager.getLogger(getClass());
	private SendMailService sendMailService = new SendMailServiceImpl();
	private ReportPreviewService reportPreviewService = new ReportPreviewServiceImpl();

	private final String ERROR_SUBJECT = "Error job: ";

	private final String CSV_DATE_FORMAT = "csv.date.format";
	private final String CSV_DATE_TIME_FORMAT = "csv.date-time.format";
	private final String CSV_TIME_FORMAT = "csv.time.format";

	private final String TABLE_STYLE = "border-collapse:collapse;font-family:Arial,sans-serif;font-size:12px";
	private final String HEADER_CELL_STYLE = "background-color:#e0e0e0;font-weight:bold;border:1px solid #b0b0b0;padding:4px 8px;text-align:left";
	private final String CELL_STYLE = "border:1px solid #b0b0b0;padding:4px 8px";
	private final String EMPTY_CELL = "&nbsp;";

	private final String ERROR = "NoImport";
	private final String ERROR_FILE_DELIMITER = ",";

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		jobId = context == null ? jobId
				: UUID.fromString(context.getJobDetail().getJobDataMap().getString(JobItemUpdate.JOB_ID));

		reportId = context == null ? reportId
				: StringUtils.hasText(context.getJobDetail().getJobDataMap().getString(JobItemUpdate.REPORT_ID))
						? UUID.fromString(context.getJobDetail().getJobDataMap().getString(JobItemUpdate.REPORT_ID))
						: null;

		modelId = context == null ? modelId
				: StringUtils.hasText(context.getJobDetail().getJobDataMap().getString(JobItemUpdate.MODEL_ID))
						? UUID.fromString(context.getJobDetail().getJobDataMap().getString(JobItemUpdate.MODEL_ID))
						: null;

		JobItem jobItem = this.databaseService.findById(jobId, JobItem.class);

		JobLog jobLog = new JobLog();
		jobLog.setJobItem(jobItem);
		jobLog.setStartTime(LocalDateTime.now());
		jobLog.setStatus(JobLogStatus.IN_PROGRESS);
		jobLog = this.databaseService.save(jobLog);

		Boolean isSuccefull = true;
		Exception exception = null;

		try {
			if (this.reportId != null) {
				Report report = this.databaseService.findById(reportId, Report.class);
				switch (report.getType()) {
				case EXECUTE:
					getExecuteJob(jobItem, report);
					break;
				case JASPER:
					getJasperJob(jobItem, report);
					break;
				case SQL:
					getSqlJob(jobItem, report);
					break;

				}
			}

			if (this.modelId != null) {
				Model model = this.databaseService.findById(modelId, Model.class);
				if (!jobItem.getJobType().equals(JobType.IMPORT_FILE))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongType", jobItem.getJobType());

				ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(this.modelId);
				if (!modelDTO.getType().equals(ModelType.TABLE) || UpdateObject.tableHasParent(modelDTO))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "importOnlyTopTable", null);
				getExecuteImportFile(jobItem, model);
			}

		} catch (Exception e) {
			Exception lastException = CustomDefaultExceptionMapper.getLastException(e);
			logger.error(lastException.getMessage(), lastException);
			exception = lastException;
			isSuccefull = false;
		} finally {
			jobLog.setEndTime(LocalDateTime.now());
			jobLog.setStatus(isSuccefull ? JobLogStatus.SUCCESS : JobLogStatus.FAIL);
			if (exception != null) {
				String message = exception.getMessage() + "\n";
				message += Arrays.asList(exception.getStackTrace()).stream().filter(a -> a != null)
						.map(a -> a.toString() + "\n").collect(Collectors.joining());
				jobLog.setErrorText(message);
			}
			this.databaseService.save(jobLog);

			if (!isSuccefull)
				if (StringUtils.hasText(jobItem.getErrorReceive()) && StringUtils.notNull(jobItem.getSmtp())) {
					String message = exception.getMessage() + "<br>";
					message += Arrays.asList(exception.getStackTrace()).stream().filter(a -> a != null)
							.map(a -> a.toString() + "<br>").collect(Collectors.joining());
					this.sendMailService.sendMail(jobItem.getSmtp(), jobItem.getErrorReceive(),
							ERROR_SUBJECT + jobItem.getName(), message);
				}

		}

		if (!isSuccefull) {
			throw new WebApplicationException(exception);
		}
	}

	private void getExecuteJob(JobItem jobItem, Report report) {
		LinkedHashMap<String, Object> parameters = getParameters(report.getId());
		this.reportPreviewService.getReportExecute(parameters, report.getId(), false);

	}

	private void getExecuteImportFile(JobItem jobItem, Model model) {

		File rootFile = new File(jobItem.getFilePath());
		File backupFolder = new File(rootFile.getAbsolutePath() + this.BACKUP_FOLDER);
		File errorFolder = new File(rootFile.getAbsolutePath() + this.ERROR_FOLDER);

		List<File> errorFile = new ArrayList<>();
		if (!(checkFolderExists(rootFile)))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noPathFound: " + jobItem.getFilePath(),
					null);

		List<File> files = Arrays.asList(rootFile.listFiles()).stream().filter(a -> a.isFile()).filter(a -> {
			if (StringUtils.hasText(jobItem.getFileName())) {
				return a.getName().startsWith(jobItem.getFileName());
			} else {
				return true;
			}
		}).filter(a -> UpdateObject.findMimeType(a).equals(ExportTableServiceImpl.EXCEL_CONTENT_TYPE))
				.collect(Collectors.toList());

		for (File file : files) {
			try {

				ModelTemplateUpload modelTemplateUpload = new ModelTemplateUpload(null, null, modelId, null, file,
						false);
				this.databaseService.executeQuery(modelTemplateUpload);

				if (!checkFolderExists(backupFolder))
					backupFolder.mkdir();

				try {
					Files.move(Paths.get(file.getAbsolutePath()),
							Paths.get(backupFolder.getAbsolutePath() + "/" + file.getName()),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					throw new WebApplicationException(e1);
				}

			} catch (Exception e) {
				errorFile.add(file);
				this.logger.error("NoFileImport: " + file.getName());
				Exception lastException = CustomDefaultExceptionMapper.getLastException(e);
				logger.error(lastException.getMessage(), lastException);

				if (!checkFolderExists(errorFolder))
					errorFolder.mkdir();

				try {
					Files.move(Paths.get(file.getAbsolutePath()),
							Paths.get(errorFolder.getAbsolutePath() + "/" + file.getName()),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					throw new WebApplicationException(e1);
				}

			}
		}

		if (!errorFile.isEmpty())
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, this.ERROR + ": "
					+ errorFile.stream().map(a -> a.getName()).collect(Collectors.joining(ERROR_FILE_DELIMITER)), null);
	}

	private Boolean checkFolderExists(File folder) {
		if (!folder.exists())
			return false;

		if (!folder.isDirectory())
			return false;

		return true;
	}

	private void getSqlJob(JobItem jobItem, Report report) {
		LinkedHashMap<String, Object> parameters = getParameters(report.getId());

		if (jobItem.getJobType().equals(JobType.SEND_MAIL)) {
			if (StringUtils.isNull(jobItem.getReceive()))
				return;

			switch (jobItem.getJobOutputType()) {
			case ATTACHMENT:
				switch (jobItem.getExportType()) {
				case CSV:
					ReportResultDTO result = this.reportPreviewService.getReportPreviewTable(parameters, report.getId(),
							false);
					if (result.getList().isEmpty() && jobItem.getConditional())
						return;

					byte[] csvByte = createCsvFile(result, jobItem.getCsvDelimiter());
					this.sendMailService.sendMail(jobItem.getSmtp(), jobItem.getReceive(), jobItem.getMailSubject(),
							jobItem.getMailBody(), csvByte, jobItem.getFileName());
					break;
				case EXCEL:
					Response response = this.reportPreviewService.getReportPreviewExcel(parameters, report.getId(),
							false);
					Integer numberOfRows = Integer
							.valueOf(response.getHeaderString(ReportPreviewServiceImpl.NUMBER_OF_ROWS));
					if (numberOfRows == 0 && jobItem.getConditional())
						return;
					byte[] excelFile = (byte[]) response.getEntity();
					this.sendMailService.sendMail(jobItem.getSmtp(), jobItem.getReceive(), jobItem.getMailSubject(),
							jobItem.getMailBody(), excelFile, jobItem.getFileName());
					break;

				}
				break;
			case INLINE:
				ReportResultDTO result = this.reportPreviewService.getReportPreviewTable(parameters, report.getId(),
						false);
				if (result.getList().isEmpty() && jobItem.getConditional())
					return;

				this.sendMailService.sendMail(jobItem.getSmtp(), jobItem.getReceive(), jobItem.getMailSubject(),
						createHtmlBody(result, jobItem.getMailBody()));
				break;

			}
		} else if (jobItem.getJobType().equals(JobType.EXPORT_FILE)) {
			File folder = new File(jobItem.getFilePath());
			if (!folder.exists())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noPathFound", jobItem.getFilePath());
			if (!folder.isDirectory())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noPathFound", jobItem.getFilePath());
			switch (jobItem.getExportType()) {
			case CSV:
				ReportResultDTO result = this.reportPreviewService.getReportPreviewTable(parameters, report.getId(),
						false);
				if (result.getList().isEmpty() && jobItem.getConditional())
					return;
				byte[] csvByte = createCsvFile(result, jobItem.getCsvDelimiter());
				try {
					FileUtils.writeByteArrayToFile(new File(folder.getAbsolutePath() + "/" + jobItem.getFileName()),
							csvByte);
				} catch (IOException e) {
					throw new WebApplicationException(e);
				}

				break;
			case EXCEL:
				Response response = this.reportPreviewService.getReportPreviewExcel(parameters, report.getId(), false);
				Integer numberOfRows = Integer
						.valueOf(response.getHeaderString(ReportPreviewServiceImpl.NUMBER_OF_ROWS));
				if (numberOfRows == 0 && jobItem.getConditional())
					return;
				byte[] excelFile = (byte[]) response.getEntity();
				try {
					FileUtils.writeByteArrayToFile(new File(folder.getAbsolutePath() + "/" + jobItem.getFileName()),
							excelFile);
				} catch (IOException e) {
					throw new WebApplicationException(e);
				}
				break;

			}
		}
	}

	private String formatValue(Object value, ReportResultColumnDTO column) {
		if (StringUtils.isNull(value))
			return "";

		switch (column.getColumnType()) {
		case FILE:
			return "";
		case LOCALDATE:
			return DateTimeFormatter.ofPattern(StaticData.appProperties.getProperty(CSV_DATE_FORMAT))
					.format((LocalDate) value);
		case LOCALDATETIME:
			return DateTimeFormatter.ofPattern(StaticData.appProperties.getProperty(CSV_DATE_TIME_FORMAT))
					.format((LocalDateTime) value);
		case LOCALTIME:
			return DateTimeFormatter.ofPattern(StaticData.appProperties.getProperty(CSV_TIME_FORMAT))
					.format((LocalTime) value);
		case UUID, STRING, LONG, INTEGER, BOOLEAN, BIGDECIMAL:
			return value.toString();
		}

		return "";
	}

	private String createHtmlBody(ReportResultDTO result, String mailBody) {
		StringBuilder html = new StringBuilder();

		if (StringUtils.hasText(mailBody))
			html.append(mailBody).append("<br>");

		html.append("<table style=\"").append(TABLE_STYLE).append("\">");

		html.append("<thead><tr>");
		for (ReportResultColumnDTO column : result.getColumns())
			html.append("<th style=\"").append(HEADER_CELL_STYLE).append("\">").append(escapeHtml(column.getName()))
					.append("</th>");
		html.append("</tr></thead>");

		html.append("<tbody>");
		for (LinkedHashMap<String, Object> item : result.getList()) {
			html.append("<tr>");
			for (ReportResultColumnDTO column : result.getColumns()) {
				String value = escapeHtml(formatValue(item.get(column.getCode()), column));
				html.append("<td style=\"").append(CELL_STYLE).append("\">")
						.append(value.isEmpty() ? EMPTY_CELL : value).append("</td>");
			}
			html.append("</tr>");
		}
		html.append("</tbody></table>");

		return html.toString();
	}

	private String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void getJasperJob(JobItem jobItem, Report report) {
		LinkedHashMap<String, Object> parameters = getParameters(report.getId());
		byte[] response = (byte[]) this.reportPreviewService.getReportPreviewJasper(parameters, report.getId(), false)
				.getEntity();
		if (jobItem.getJobType().equals(JobType.SEND_MAIL)) {
			if (StringUtils.isNull(jobItem.getReceive()))
				return;

			this.sendMailService.sendMail(jobItem.getSmtp(), jobItem.getReceive(), jobItem.getMailSubject(),
					jobItem.getMailBody(), response, jobItem.getFileName());
		} else if (jobItem.getJobType().equals(JobType.EXPORT_FILE)) {
			File folder = new File(jobItem.getFilePath());
			if (!folder.exists())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noPathFound", jobItem.getFilePath());
			if (!folder.isDirectory())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noPathFound", jobItem.getFilePath());
			try {
				FileUtils.writeByteArrayToFile(new File(folder.getAbsolutePath() + "/" + jobItem.getFileName()),
						response);
			} catch (IOException e) {
				throw new WebApplicationException(e);
			}
		}
	}

	private byte[] createCsvFile(ReportResultDTO result, String delimiter) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

			String[] titles = result.getColumns().stream().map(a -> a.getName()).toArray(String[]::new);
			bufferedWriter.write(Arrays.asList(titles).stream().collect(Collectors.joining(delimiter)));
			bufferedWriter.newLine();

			for (LinkedHashMap<String, Object> item : result.getList()) {
				String[] row = new String[result.getColumns().size()];
				int columnRow = -1;
				for (ReportResultColumnDTO column : result.getColumns()) {
					columnRow++;
					Object value = item.get(column.getCode());
					if (StringUtils.isNull(value)) {
						row[columnRow] = "";
						continue;
					}
					switch (column.getColumnType()) {
					case FILE:
						row[columnRow] = "";
						break;
					case LOCALDATE:
						LocalDate date = (LocalDate) value;
						row[columnRow] = DateTimeFormatter
								.ofPattern(StaticData.appProperties.getProperty(CSV_DATE_FORMAT)).format(date);
						break;
					case LOCALDATETIME:
						LocalDateTime dateDateTime = (LocalDateTime) value;
						row[columnRow] = DateTimeFormatter
								.ofPattern(StaticData.appProperties.getProperty(CSV_DATE_TIME_FORMAT))
								.format(dateDateTime);
						break;
					case LOCALTIME:
						LocalTime time = (LocalTime) value;
						row[columnRow] = DateTimeFormatter
								.ofPattern(StaticData.appProperties.getProperty(CSV_TIME_FORMAT)).format(time);
						break;
					case UUID, STRING, LONG, INTEGER, BOOLEAN, BIGDECIMAL:
						row[columnRow] = value.toString();
						break;

					}
				}
				bufferedWriter.write(Arrays.asList(row).stream().collect(Collectors.joining(delimiter)));
				bufferedWriter.newLine();
			}

			bufferedWriter.close();
			return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	private LinkedHashMap<String, Object> getParameters(UUID reportId) {
		ReportInfoDTO reportInfoDTO = reportPreviewService.getReportInfo(reportId, false);
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		for (ReportInfoParameterDTO parameter : reportInfoDTO.getParameters()) {
			parameters.put(parameter.getCode(), parameter.getDefaultValue());
		}
		return parameters;
	}
}
