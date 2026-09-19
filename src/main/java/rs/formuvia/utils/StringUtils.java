package rs.formuvia.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {
	
	public static Boolean hasText(String text) {
		
		if(text==null)
			return false;
		
		if(text.trim().length()==0) {
			return false;
		}
		
		return true;
	}
	
	public static Boolean notNull(Object object) {
		
		if(object==null)
			return false;
		
		if(object.toString().trim().length()==0) {
			return false;
		}
		
		return true;
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
