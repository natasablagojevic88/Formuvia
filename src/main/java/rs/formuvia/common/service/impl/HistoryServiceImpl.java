package rs.formuvia.common.service.impl;

import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.common.service.HistoryService;
import rs.formuvia.common.utils.CreateHistory;
import rs.formuvia.database.service.DatabaseService;

@Service
public class HistoryServiceImpl implements HistoryService {

	@Inject
	private DatabaseService databaseService;

	@Context
	private HttpServletRequest httpServletRequest;

	@Override
	public List<HistoryDTO> getHistory(String className, UUID id) {
		CreateHistory createHistory = new CreateHistory(httpServletRequest, className, id);
		return databaseService.executeQuery(createHistory);

	}

}
