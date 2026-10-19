package rs.formuvia.report.service;

import java.util.LinkedHashMap;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportResultDTO;

public interface ReportPreviewService {

	ReportResultDTO getReportPreviewTable(LinkedHashMap<String, Object> parameters, UUID reportId);

	Response getReportPreviewExcel(LinkedHashMap<String, Object> parameters, UUID reportId);

	void getReportExecute(LinkedHashMap<String, Object> parameters, UUID reportId);

	ReportInfoDTO getReportInfo(UUID reportId);

}
