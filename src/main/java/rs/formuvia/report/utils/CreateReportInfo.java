package rs.formuvia.report.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportInfoParameterDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.service.impl.ReportPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateReportInfo implements ExecuteQuery<ReportInfoDTO> {
	private final HttpServletRequest httpServletRequest;
	private final UUID reportId;

	private ResourceBundleService resourceBundleService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public CreateReportInfo(HttpServletRequest httpServletRequest, UUID reportId) {
		this.httpServletRequest = httpServletRequest;
		this.reportId = reportId;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public ReportInfoDTO execute(Connection connection) throws Exception {
		ReportDTO reportDTO = ReportPreviewServiceImpl.findReport(reportId);
		ReportInfoDTO reportInfoDTO = new ReportInfoDTO();
		reportInfoDTO.setTitle(this.resourceBundleService.getText(reportDTO.getName()));
		reportInfoDTO.setDescription(this.resourceBundleService.getText(reportDTO.getDescription()));
		reportInfoDTO.setReportType(reportDTO.getType());

		List<ReportParameterDTO> list = getParametersForReportId(this.reportId);
		for (ReportParameterDTO parameter : list) {
			ReportInfoParameterDTO reportInfoParameterDTO = new ReportInfoParameterDTO();
			reportInfoParameterDTO.setCode(parameter.getCode());
			reportInfoParameterDTO.setNullable(parameter.getNullable());

			if (StringUtils.notNull(parameter.getDefaultValue()))
				reportInfoParameterDTO.setDefaultValue(this.databaseService
						.executeNativeQuery(parameter.getDefaultValue(), null, Object.class, connection).getFirst());

			if (StringUtils.notNull(parameter.getListOfValues())) {
				List<Object[]> listOfValues = this.databaseService.executeNativeQuery(parameter.getListOfValues(), null,
						Object[].class, connection);
				reportInfoParameterDTO.setListOfValues(
						listOfValues.stream().map(a -> new ComboboxDTO(a[0], a[1].toString())).toList());
			}
			reportInfoParameterDTO.setName(this.resourceBundleService.getText(parameter.getName()));
			reportInfoParameterDTO.setType(parameter.getColumnType());
			reportInfoDTO.getParameters().add(reportInfoParameterDTO);
		}

		return reportInfoDTO;
	}

	private static List<ReportParameterDTO> getParametersForReportId(UUID reportId) {
		return StaticData.reportParameters.stream().filter(a -> a.getReportId().equals(reportId))
				.collect(Collectors.toList());
	}

	public static ReportParameterDTO getParametersFromList(UUID reportId, String name) {
		return getParametersForReportId(reportId).stream().filter(a -> a.getCode().equals(name)).findFirst().get();
	}

}
