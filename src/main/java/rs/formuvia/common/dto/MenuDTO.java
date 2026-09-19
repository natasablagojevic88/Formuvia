package rs.formuvia.common.dto;

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
public class MenuDTO {

	private String icon;

	private String name;

	private String url;

	private List<MenuDTO> children = new ArrayList<>();
}
