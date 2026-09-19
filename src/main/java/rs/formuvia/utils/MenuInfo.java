package rs.formuvia.utils;

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
public class MenuInfo {

	private String icon;

	private String name;

	private String url;

	private String role;

	private List<MenuInfo> items = new ArrayList<>();
}
