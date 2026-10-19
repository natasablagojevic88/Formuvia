package rs.formuvia.report.service;

import java.util.UUID;

import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.report.dto.ReportGroupDTO;

public interface ReportGroupService {

	DatabaseTable<ReportGroupDTO> createTable(DatabaseParameter databaseParameter);

	ReportGroupDTO getReportGroup(UUID id);

	ReportGroupDTO getUpdate(ReportGroupDTO reportGroupDTO);

	void getDelete(UUID id);

}
