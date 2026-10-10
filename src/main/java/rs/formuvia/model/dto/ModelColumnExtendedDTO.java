package rs.formuvia.model.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModelColumnExtendedDTO extends ModelColumnDTO {

	private List<ModelColumnConditionDTO> conditions = new ArrayList<>();

}
