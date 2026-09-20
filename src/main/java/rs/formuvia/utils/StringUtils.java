package rs.formuvia.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {
	
	public static Boolean hasText(String text) {
		
		if(text==null)
			return false;
		
		if(text.trim().isBlank()) {
			return false;
		}
		
		if(text.trim().isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	public static Boolean notNull(Object object) {
		
		if(object==null)
			return false;
		
		if(object.toString().trim().isEmpty()) {
			return false;
		}
		
		if(object.toString().trim().isBlank()) {
			return false;
		}
		
		return true;
	}
	
	public static Boolean isNull(Object object) {
		if(object==null) 
			return true;
		
		if(object.toString().trim().isEmpty()) {
			return true;
		}
		
		if(object.toString().trim().isBlank()) {
			return true;
		}
		
		return false;
	}
	
	public static String createStringArray(String[] array) {
		List<String> list=Arrays.asList(array);
		
		if(list.isEmpty()) {
			return "";
		}
		
		if(list.size()==1) {
			return list.getFirst();
		}
		
		return list.stream()
				.collect(Collectors.joining(","));
		
	}
	
	public static String createStringArrayWithDelimiter(String[] array,String delimiter) {
		List<String> list=Arrays.asList(array);
		
		if(list.isEmpty()) {
			return "";
		}
		
		if(list.size()==1) {
			return list.getFirst();
		}
		
		return list.stream()
				.collect(Collectors.joining(delimiter));
		
	}

}
