package rs.formuvia.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Scheduler;

import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;

public class StaticData {

	public static List<Class<?>> allClasses = new ArrayList<>();
	public static Map<Class<?>, List<Field>> classFields = new ConcurrentHashMap<>();
	public static Map<String, Class<?>> allClassesByName = new ConcurrentHashMap<>();

	public static Properties appProperties = new Properties();
	public static BlockingQueue<Connection> connections = new LinkedBlockingQueue<>();
	public static Set<Connection> allConnections = ConcurrentHashMap.newKeySet();

	public static Scheduler localScheduler;
	public static Scheduler databaseScheduler;
	public static Connection databaseListenConnection;
	public static List<MenuInfo> menuInfos = new ArrayList<>();

	public static List<AppUser> appUsers = new ArrayList<>();
	public static List<AppUserRoleDTO> appUserRoles = new ArrayList<>();
	public static List<RoleDTO> roles = new ArrayList<>();
	public static List<ModelDTO> models = new ArrayList<>();
	public static List<ModelColumnDTO> modelColumns = new ArrayList<>();

	public static Map<UUID, List<ComboboxDTO>> modelCodebook = new ConcurrentHashMap<>();
	public static Set<UUID> modelsToListen = ConcurrentHashMap.newKeySet();

}
