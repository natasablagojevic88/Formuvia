package rs.formuvia.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Scheduler;

import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;

public class StaticData {

	public static List<Class<?>> allClasses = new ArrayList<>();
	public static Map<Class<?>, List<Field>> classFields = new HashMap<>();
	public static Map<String,Class<?>> allClassesByName = new HashMap<>();
	
	public static Properties appProperties = new Properties();
	public static BlockingQueue<Connection> connections = new LinkedBlockingQueue<>();
	public static Set<Connection> allConnections = new HashSet<>();
	
	public static Scheduler localScheduler;
	public static Scheduler databaseScheduler;
	public static Connection databaseListenConnection;
	public static List<MenuInfo> menuInfos = new ArrayList<>();

	public static List<AppUser> appUsers = new ArrayList<>();
	public static List<AppUserRoleDTO> appUserRoles = new ArrayList<>();
	public static List<RoleDTO> roles = new ArrayList<>();

}
