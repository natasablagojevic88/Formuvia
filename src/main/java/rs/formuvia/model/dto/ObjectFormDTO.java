package rs.formuvia.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.dto.ComboboxDTO;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectFormDTO {

	private List<ModelColumnPreviewDTO> fields = new ArrayList<>();

	private Map<UUID, List<ComboboxDTO>> parentCodebook = new ConcurrentHashMap<>();
}
