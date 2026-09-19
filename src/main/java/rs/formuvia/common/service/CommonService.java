package rs.formuvia.common.service;

import java.util.Locale;
import java.util.Set;

import rs.formuvia.administration.entity.AppUser;

public interface CommonService {

	String readQueryFromFile(String fileName);
	
	Locale getUserLocale();
	
	void setUserLocale(Locale locale);
	
	AppUser getUser();
	
	void setIpAdress(String ipAdress);
	
	String getIpAdress();
	
	Set<String> getRoles();
}
