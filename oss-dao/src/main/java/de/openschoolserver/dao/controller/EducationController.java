/* (c) 2017 Péter Varkoly <peter@varkoly.de> - all rights reserved  */
package de.openschoolserver.dao.controller;

import java.io.File;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.openschoolserver.dao.*;
import de.openschoolserver.dao.tools.OSSShellTools;

public class EducationController extends Controller {

	Logger logger = LoggerFactory.getLogger(EducationController.class);
    static FileAttribute<Set<PosixFilePermission>> privatDirAttribute  = PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString("rwx------"));
    static FileAttribute<Set<PosixFilePermission>> privatFileAttribute = PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString("rw-------"));
    //TODO
    static String OSS_CLIENT_CONTROL_FILE = "OSS_CLIENT_CONTROL_FILE";
	
	public EducationController(Session session) {
		super(session);
	}

	/*
	 * Return the Category to a smart room
	 */
	
	public Long getCategoryToRoom(Long roomId){
		EntityManager   em = getEntityManager();
		Room room;
		try {
			room = em.find(Room.class, roomId);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		} finally {
			em.close();
		}
		for( Category category : room.getCategories() ) {
			if( room.getName().equals(category.getName()) && category.getCategoryType().equals("smartRoom")) {
				return category.getId();
			}
		}
		return null;
	}

	/*
	 * Return the list of ids of rooms in which a user may actually control the access.
	 */
	public List<Long> getMyRooms() {
		List<Long> rooms = new ArrayList<Long>();
		if( this.session.getRoom().getRoomControl().equals("no_control")){
			for( Room room : new RoomController(this.session).getAll() ) {
				switch(room.getRoomControl()) {
				case "no_control":
					break;
				case "all_teacher_control":
					rooms.add(room.getId());
					break;
				case "teacher_control":
					if( this.checkMConfig(room, "teacher_control", Long.toString((this.session.getUserId())))) {
						rooms.add(room.getId());
					}
				}
			}
		} else {
			rooms.add(this.session.getRoomId());
		}
		for( Category category : this.session.getUser().getCategories() ) {
			for( Room room : category.getRooms() ) {
				if( room.getRoomType().equals("smartRoom")) {
					rooms.add(room.getId());
				}
			}
		}
		return rooms;
	}

	/*
	 * Create the a new smart room from a hash:
	 * {
	 *     "name"  : <Smart room name>,
	 *     "description : <Descripton of the room>,
	 *     "studentsOnly : true/false
	 * }
	 */
	public Response createSmartRoom(Category smartRoom) {
		EntityManager   em = getEntityManager();
		User   owner       = this.session.getUser();
		/* Define the room */
		Room     room      = new Room();
		room.setName(smartRoom.getName());
		room.setDescription(smartRoom.getDescription());
		room.setRoomType("smartRoom");
		room.getCategories().add(smartRoom);
		smartRoom.getRooms().add(room);
		smartRoom.setOwner(owner);
		smartRoom.setCategoryType("smartRoom");
		owner.getCategories().add(smartRoom);

		try {
			em.getTransaction().begin();
			em.persist(room);
			em.persist(smartRoom);
			em.merge(owner);
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			em.close();
			return new Response(this.getSession(),"ERROR", e.getMessage());
		}
		try {
			em.getTransaction().begin();
			/*
			 * Add groups to the smart room
			 */
			GroupController groupController = new GroupController(this.session);
			for( Long id : smartRoom.getGroupIds()) {
				Group group = groupController.getById(id);
				smartRoom.getGroups().add(group);
				group.getCategories().add(smartRoom);
				em.merge(room);
				em.merge(smartRoom);
			}
			/*
			 * Add users to the smart room
			 */
			UserController  userController  = new UserController(this.session);
			for( Long id : smartRoom.getUserIds()) {
				User user = userController.getById(Long.valueOf(id));
				if(smartRoom.getStudentsOnly() && ! user.getRole().equals("studetns")){
					continue;
				}
				smartRoom.getUsers().add(user);
				user.getCategories().add(smartRoom);
				em.merge(user);
				em.merge(smartRoom);
			}
			/*
			 * Add devices to the smart room
			 */
			DeviceController deviceController = new DeviceController(this.session);
			for( Long id: smartRoom.getDeviceIds()) {
				Device device = deviceController.getById(Long.valueOf(id));
				smartRoom.getDevices().add(device);
				device.getCategories().add(smartRoom);
				em.merge(device);
				em.merge(smartRoom);
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR", e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Smart Room was created succesfully"); 
	}

	public Response modifySmartRoom(long roomId, Category smartRoom) {
		EntityManager   em = getEntityManager();
		try {
			em.getTransaction().begin();
			Room room = smartRoom.getRooms().get(0);
			room.setName(smartRoom.getName());
			room.setDescription(smartRoom.getDescription());
			em.merge(smartRoom);
			em.merge(room);
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR", e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Smart Room was modified succesfully");
	}
	
	public Response deleteSmartRoom(Long roomId) {
		EntityManager   em = getEntityManager();
		try {
			em.getTransaction().begin();
			Room room         = new RoomController(this.session).getById(roomId);
			Category category = room.getCategories().get(0);
			em.merge(room);
			em.remove(room);
			em.merge(category);
			em.remove(category);
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR", e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Smart Room was deleted succesfully");
	}

	
	/*
	 * Get the list of users which are logged in a room or smart room
	 */
	public List<List<Long>> getRoom(long roomId) {
		List<List<Long>> loggedOns = new ArrayList<List<Long>>();
		List<Long> loggedOn;
		RoomController roomController = new RoomController(this.session);
		Room room = roomController.getById(roomId);
		User me   = this.session.getUser();
		if( room.getRoomType().equals("smartRoom")) {
			Category category = room.getCategories().get(0);
			for( Group group : category.getGroups() ) {
				for( User user : group.getUsers() ) {
					if(	category.getStudentsOnly() && ! user.getRole().equals("studetns") ||
						user.equals(me)	){
						continue;
					}
					for( Device device : user.getLoggedOn() ) {
						loggedOn = new ArrayList<Long>();
						loggedOn.add(user.getId());
						loggedOn.add(device.getId());
						loggedOns.add(loggedOn);
					}
				}
			}
			for( User user : category.getUsers() ) {
				if( user.equals(me) ) {
					continue;
				}
				for( Device device : user.getLoggedOn() ) {
						loggedOn = new ArrayList<Long>();
						loggedOn.add(user.getId());
						loggedOn.add(device.getId());
						loggedOns.add(loggedOn);
				}
			}
			for( Device device : category.getDevices() ) {
				for( User user : device.getLoggedIn() ) {
					if( ! user.equals(me) ) {
						loggedOn = new ArrayList<Long>();
						loggedOn.add(user.getId());
						loggedOn.add(device.getId());
						loggedOns.add(loggedOn);
					}
				}
			}
		} else {
			for( Device device : room.getDevices() ) {
				for( User user : device.getLoggedIn() ) {
					if( ! user.equals(me) ) {
						loggedOn = new ArrayList<Long>();
						loggedOn.add(user.getId());
						loggedOn.add(device.getId());
						loggedOns.add(loggedOn);
					}
				}
			}
		}
		return loggedOns;
	}

	public Response uploadFileTo(String what, long objectId, InputStream fileInputStream,
			FormDataContentDisposition contentDispositionHeader) {
		String fileName = contentDispositionHeader.getFileName();
		File file = null;
		try {
			file = File.createTempFile("oss_uploadFile", ".ossb", new File("/opt/oss-java/tmp/"));
			Files.copy(fileInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return new Response(this.getSession(),"ERROR", e.getMessage());
		}
		StringBuilder error = new StringBuilder();
		switch(what) {
		case "user":
			User user = new UserController(this.session).getById(objectId);
			error.append(this.saveFileToUserImport(user, file, fileName));
			break;
		case "group":
			Group group = new GroupController(this.session).getById(objectId);
			for( User myUser : group.getUsers() ) {
				error.append(this.saveFileToUserImport(myUser, file, fileName));
			}
			break;
		case "device":
			Device device = new DeviceController(this.session).getById(objectId);
			for( User myUser : device.getLoggedIn() ) {
				error.append(this.saveFileToUserImport(myUser, file, fileName));
			}
			break;
		case "room":
			UserController userController = new UserController(this.session);
			for( List<Long> loggedOn : this.getRoom(objectId) ) {
				User myUser = userController.getById(loggedOn.get(0));
				error.append(this.saveFileToUserImport(myUser, file, fileName));
			}
		}
		file.delete();
		return new Response(this.getSession(),"OK", "File was copied succesfully.");
	}
	
	public String saveFileToUserImport(User user, File file, String fileName) {
		UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		try {
			StringBuilder newFileName = new StringBuilder(this.getConfigValue("SCHOOL_HOME_BASE"));
			newFileName.append("/").append(user.getUid()).append("/") .append("Import").append("/");
			// Create the directory first.
			File importDir = new File( newFileName.toString() );
			Files.createDirectories(importDir.toPath(), privatDirAttribute );

			// Copy temp file to the rigth place
			newFileName.append(fileName);
			File newFile = new File( newFileName.toString() );
			Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			// Set owner
			UserPrincipal owner = lookupService.lookupPrincipalByName(user.getUid());
			Files.setOwner(importDir.toPath(), owner);
			Files.setOwner(newFile.toPath(), owner);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return e.getMessage() + System.lineSeparator();
		}
		return "";
	}

	public Response collectFileFromUser(User user,String project, boolean cleanUpExport, boolean sortInDirs) {
		String[] program = new String[11];
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		program[0] = "/usr/sbin/oss_collect_files.sh";
		program[1] = "-t";
		program[2] = this.session.getUser().getUid();
		program[3] = "-f";
		program[4] = user.getUid();
		program[5] = "-p";
		program[6] = project;
		program[7] = "-c";
		if( cleanUpExport ) {
			program[8] = "y";
		} else {
			program[8] = "n";
		}
		program[9] = "-d";
		if( sortInDirs ) {
			program[10] = "y";
		} else {
			program[10] = "n";
		}
		OSSShellTools.exec(program, reply, stderr, null);
		if( stderr.toString().isEmpty() ) {
			return new Response(this.getSession(),"OK", "File was collected from:" + user.getUid() );
		}
		return new Response(this.getSession(),"ERROR", stderr.toString());
	}

	public Response createGroup(Group group) {
		GroupController groupController = new GroupController(this.session);
		group.setGroupType("workgroup");
		group.setOwner(session.getUser());
		return groupController.add(group);
	}

	public Response modifyGroup(long groupId, Group group) {
		GroupController groupController = new GroupController(this.session);
		Group emGroup = groupController.getById(groupId);
		if( this.session.getUser().equals(emGroup.getOwner())) {
			return groupController.modify(group);
		} else {
			return new Response(this.getSession(),"ERROR", "You are not the owner of this group.");
		}
	}

	public Response deleteGroup(long groupId) {
		GroupController groupController = new GroupController(this.session);
		Group emGroup = groupController.getById(groupId);
		if( this.session.getUser().equals(emGroup.getOwner())) {
			return groupController.delete(groupId);
		} else {
			return new Response(this.getSession(),"ERROR", "You are not the owner of this group.");
		}
	}

	public List<String> getAvailableRoomActions(long roomId) {
		Room room = new RoomController(this.session).getById(roomId);
		List<String> actions = new ArrayList<String>();
		for( String action : this.getProperty("de.openschoolserver.dao.EducationController.RoomActions").split(",") ) {
			if(! this.checkMConfig(room, "disabledActions", action )) {
				actions.add(action);
			}
		}
		return actions;
	}

	public List<String> getAvailableUserActions(long userId) {
		User user = new UserController(this.session).getById(userId);
		List<String> actions = new ArrayList<String>();
		for( String action : this.getProperty("de.openschoolserver.dao.EducationController.UserActions").split(",") ) {
			if(! this.checkMConfig(user, "disabledActions", action )) {
				actions.add(action);
			}
		}
		return actions;
	}

	public List<String> getAvailableDeviceActions(long deviceId) {
		Device device = new DeviceController(this.session).getById(deviceId);
		List<String> actions = new ArrayList<String>();
		for( String action : this.getProperty("de.openschoolserver.dao.EducationController.DeviceActions").split(",") ) {
			if(! this.checkMConfig(device, "disabledActions", action )) {
				actions.add(action);
			}
		}
		return actions;
	}


	public Response manageRoom(long roomId, String action, Map<String, String> actionContent) {
		Response response = null;
		List<String> errors = new ArrayList<String>();
		for( List<Long> loggedOn : this.getRoom(roomId) ) {
			if(this.session.getDevice().getIp().equals(loggedOn.get(1)) ||
			   this.session.getDevice().getWlanIp().equals(loggedOn.get(1))
			  ) {
				continue;
			}
			response = this.manageDevice(loggedOn.get(1), action, actionContent);
			if( response.getCode().equals("ERROR")) {
				errors.add(response.getValue());
			}
		}
		if( errors.isEmpty() ) {
			new Response(this.getSession(),"OK", "Device control was made applied.");
		} else {
			return new Response(this.getSession(),"ERROR",String.join("<br>", errors));
		}
		return null;
	}

	public Response manageDevice(long deviceId, String action, Map<String, String> actionContent) {
		Device device = new DeviceController(this.session).getById(deviceId);
		if(this.session.getDevice().equals(device)) {
			return new Response(this.getSession(),"ERROR", "Do not control the own client.");
		}
		StringBuilder FQHN = new StringBuilder();
		FQHN.append(device.getName()).append(".").append(this.getConfigValue("SCHOOL_DOMAIN"));
		File file;
		String[] program    = null;
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		switch(action) {
		case "shutDown":
			program = new String[3];
			program[0] = "/usr/bin/salt";
			program[1] = FQHN.toString();
			program[2] = "system.shutdown";		
			break;
		case "reboot":
			program = new String[3];
			program[0] = "/usr/bin/salt";
			program[1] = FQHN.toString();
			program[2] = "system.reboot";
			break;
		case "logout":
			break;
		case "close":
			break;
		case "open":
			break;
		case "wol":
			program = new String[4];
			program[0] = "/usr/bin/wol";
			program[1] = "-i";
			program[2] = device.getIp();
			program[3] = device.getMac();
			break;
		case "controlProxy":
			break;
		case "saveFile":
			List<String>   fileContent =new ArrayList<String>();
			fileContent.add(actionContent.get("content"));
			try {
				file  = File.createTempFile("oss_", ".ossb", new File("/opt/oss-java/tmp/"));
				Files.write(file.toPath(), fileContent);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return new Response(this.getSession(),"ERROR", e.getMessage());
			}
			program = new String[4];
			program[0] = "/usr/bin/salt-cp";
			program[1] = FQHN.toString();
			program[2] = file.toPath().toString();
			program[3] = actionContent.get("path");
			break;
		default:
				return new Response(this.getSession(),"ERROR", "Unknonw action.");	
		}
		OSSShellTools.exec(program, reply, stderr, null);
		return new Response(this.getSession(),"OK", "Device control was applied.");
	}

	public Response getRoomControl(long roomId, long minutes) {
		Map<String, String> actionContent = new HashMap<String,String>();
		StringBuilder controllers = new StringBuilder();
		controllers.append(this.getConfigValue("SCHOOL_SERVER")).append(",").append(this.session.getDevice().getIp());
		if( this.session.getDevice().getWlanIp() != null ) {
			controllers.append(",").append(this.session.getDevice().getWlanIp());
		}
		actionContent.put("path",  OSS_CLIENT_CONTROL_FILE);
		actionContent.put("content", controllers.toString());
		
		Response response = this.manageRoom(roomId, "saveFile", actionContent);
		if( response.getCode().equals("OK")) {
			Room room = new RoomController(this.session).getById(roomId);
			RoomSmartControl roomSmartControl = new RoomSmartControl(roomId,this.session.getUserId(),minutes);
			EntityManager em = getEntityManager();
			try {
				em.getTransaction().begin();
				em.persist(roomSmartControl);
				em.getTransaction().commit();
			} catch (Exception e) {
				logger.error(e.getMessage());
				return new Response(this.getSession(),"ERROR", e.getMessage());
			} finally {
				em.close();
			}	
			return new Response(this.getSession(),"OK", "Now you have the control for the selected room.");
		}
		return response;
	}
}