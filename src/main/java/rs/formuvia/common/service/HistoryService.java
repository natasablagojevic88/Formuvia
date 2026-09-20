package rs.formuvia.common.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.common.dto.HistoryDTO;

public interface HistoryService {

	List<HistoryDTO> getHistory(String className, UUID id);

}
