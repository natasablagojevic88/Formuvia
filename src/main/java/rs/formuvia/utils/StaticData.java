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
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileDTO;

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

	public static volatile List<AppUser> appUsers = new ArrayList<>();
	public static volatile List<AppUserRoleDTO> appUserRoles = new ArrayList<>();
	public static volatile List<RoleDTO> roles = new ArrayList<>();
	public static volatile List<ModelDTO> models = new ArrayList<>();
	public static volatile List<ModelColumnDTO> modelColumns = new ArrayList<>();
	public static volatile List<ModelColumnConditionDTO> modelColumnsConditions = new ArrayList<>();
	public static volatile List<ModelFileDTO> modelFiles = new ArrayList<>();

	public static volatile Map<UUID, List<ComboboxDTO>> modelCodebook = new ConcurrentHashMap<>();
	public static volatile Set<UUID> modelsToListen = ConcurrentHashMap.newKeySet();

}
