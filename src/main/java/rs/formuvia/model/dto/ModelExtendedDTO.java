package rs.formuvia.model.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModelExtendedDTO extends ModelDTO {

	private List<ModelProcedureDTO> procedures = new ArrayList<>();

	private List<ModelTriggerDTO> triggers = new ArrayList<>();

	private List<ModelJasperDTO> jaspers = new ArrayList<>();
}
