package rs.formuvia.report.service;

import java.util.UUID;

import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportParameterDTO;

public interface ReportService {

	DatabaseTable<ReportDTO> getTable(DatabaseParameter databaseParameter, UUID reportGroupId);

	ReportDTO getReport(UUID id);

	ReportDTO updateReport(ReportDTO reportDTO);

	void deleteReport(UUID id);

	DatabaseTable<ReportParameterDTO> getParameterTable(DatabaseParameter databaseParameter, UUID reportId);

	ReportParameterDTO getParameterUpdate(ReportParameterDTO reportParameterDTO);

	void getParameterDelete(UUID id);

	Integer getParameterOrderNumber(UUID reportId);

}
