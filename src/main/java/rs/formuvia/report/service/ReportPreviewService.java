package rs.formuvia.report.service;

import java.util.LinkedHashMap;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportResultDTO;

public interface ReportPreviewService {

	ReportResultDTO getReportPreviewTable(LinkedHashMap<String, Object> parameters, UUID reportId, Boolean checkRole);

	Response getReportPreviewExcel(LinkedHashMap<String, Object> parameters, UUID reportId, Boolean checkRole);

	void getReportExecute(LinkedHashMap<String, Object> parameters, UUID reportId, Boolean checkRole);

	ReportInfoDTO getReportInfo(UUID reportId, Boolean checkRole);

	Response getReportPreviewJasper(LinkedHashMap<String, Object> parameters, UUID reportId, Boolean checkRole);

}
