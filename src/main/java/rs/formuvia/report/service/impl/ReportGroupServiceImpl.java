package rs.formuvia.report.service.impl;

import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.report.dto.ReportGroupDTO;
import rs.formuvia.report.entity.ReportGroup;
import rs.formuvia.report.service.ReportGroupService;
import rs.formuvia.utils.StringUtils;

@Service
public class ReportGroupServiceImpl implements ReportGroupService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public DatabaseTable<ReportGroupDTO> createTable(DatabaseParameter databaseParameter) {

		return databaseService.createTable(databaseParameter, ReportGroupDTO.class);
	}

	@Override
	public ReportGroupDTO getReportGroup(UUID id) {
		ReportGroup reportGroup = this.databaseService.findById(id, ReportGroup.class);
		return modelMapper.map(reportGroup, ReportGroupDTO.class);
	}

	@Override
	public ReportGroupDTO getUpdate(ReportGroupDTO reportGroupDTO) {
		ReportGroup reportGroup = StringUtils.isNull(reportGroupDTO.getId()) ? new ReportGroup()
				: this.databaseService.findById(reportGroupDTO.getId(), ReportGroup.class);

		DatabaseParameter databaseParameterUnique = DatabaseParameter
				.singleFilter(new DatabaseFilter[] { DatabaseFilter.valueOf("name", reportGroupDTO.getName()) });

		if (StringUtils.notNull(reportGroupDTO.getId()))
			databaseParameterUnique.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, reportGroupDTO.getId().toString()));

		if (this.databaseService.exists(databaseParameterUnique, ReportGroupDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(ReportGroupDTO.class, "name"),
					reportGroupDTO.getName());

		this.modelMapper.map(reportGroupDTO, reportGroup);

		reportGroup = this.databaseService.save(reportGroup);

		return getReportGroup(reportGroup.getId());
	}

	@Override
	public void getDelete(UUID id) {
		ReportGroup reportGroup = this.databaseService.findById(id, ReportGroup.class);
		this.databaseService.delete(reportGroup);

	}

}
