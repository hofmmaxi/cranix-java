package de.openschoolserver.dao.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.openschoolserver.dao.*;

public class SoftwareController extends Controller {
	
	Logger logger = LoggerFactory.getLogger(CloneToolController.class);

	public SoftwareController(Session session) {
		super(session);
	}
	
	public Software getById(long softwareId) {
		EntityManager em = getEntityManager();

		try {
			return em.find(Software.class, softwareId);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		} finally {
			em.close();
		}
	}

	public Map<String, String> statistic() {
		Map<String,String> statusMap = new HashMap<>();
		statusMap.put("name","software");
		EntityManager em = getEntityManager();
        Query query;
        Integer count;

        query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS","installed");
        count = query.getResultList().size();
        statusMap.put("installed", count.toString());

        query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS","installation_scheduled");
        count = query.getResultList().size();
        statusMap.put("installation_scheduled", count.toString());
        
        query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS","installation_failed");
        count = query.getResultList().size();
        statusMap.put("installation_failed", count.toString());
        
        query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS","installed_manuell");
        count = query.getResultList().size();
        statusMap.put("installed_manuell", count.toString());
        
        query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS","deinstalled_manuell");
        count = query.getResultList().size();
        statusMap.put("deinstalled_manuell", count.toString());
        
        return statusMap;
	}

	public List<Software> getAll() {
		EntityManager em = getEntityManager();
		Query query = em.createNamedQuery("Software.findAll");
		return (List<Software>)query.getResultList();
	}

	public List<SoftwareVersion> getAllVersion() {
		EntityManager em = getEntityManager();
		Query query = em.createNamedQuery("SoftwareVersion.findAll");
		return (List<SoftwareVersion>)query.getResultList();
	}
	
	public List<SoftwareStatus> getAllStatus(String installationStatus) {
		EntityManager em = getEntityManager();
        Query query = em.createNamedQuery("SoftwareStatus.findByStatus").setParameter("STATUS",installationStatus);
        return (List<SoftwareStatus>) query.getResultList();
	}
	
	public Response addSoftwareToCategory(Long softwareId,Long categoryId){
		EntityManager em = getEntityManager();
		Software s = this.getById(softwareId);
		return new Response(this.getSession(),"OK","SoftwareState was added to category succesfully");
	}
	
	public Response saveSoftwareState(){
		EntityManager em = getEntityManager();
		RoomController   roomController   = new RoomController(this.session);
		DeviceController deviceController = new DeviceController(this.session);
		
		List<String>   topSls = new ArrayList<String>();
		Path SALT_TOP_TEMPL   = Paths.get("/usr/share/oss/templates/top.sls");
		if( Files.exists(SALT_TOP_TEMPL) ) {
			try {
				topSls = Files.readAllLines(SALT_TOP_TEMPL);
			} catch (java.nio.file.NoSuchFileException e) {
				logger.error(e.getMessage());
			} catch( IOException e ) {
				logger.error(e.getMessage());
			}
		} else {
			topSls.add("base:");
		}

		//Create the workstations state files
		for( Device device : deviceController.getAll() ) {
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : device.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					String key = String.format("%04d-%s",software.getWeigth(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			if( !keys.isEmpty() ) {
				List<String> roomSls = new ArrayList<String>();
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					roomSls.add(softwareMap.get(key)+":");
					roomSls.add("  - pkg:");
					roomSls.add("    - installed:");
				}
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_device_" + device.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, roomSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + device.getName() + ":");
				topSls.add("    - oss_device_" + device.getName());
			}
		}
		//Create the room state files
		for( Room room : roomController.getAll() ) {
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : room.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					String key = String.format("%04d-%s",software.getWeigth(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			if( !keys.isEmpty() ) {
				List<String> roomSls = new ArrayList<String>();
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					roomSls.add(softwareMap.get(key)+":");
					roomSls.add("  - pkg:");
					roomSls.add("    - installed:");
				}
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_room_" + room.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, roomSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + room.getName() + ":");
				topSls.add("    - match: nodegroups");
				topSls.add("    - oss_room_" + room.getName());	
			}
		}
		
		//Create the hwconf state files
		Query query = em.createNamedQuery("HWConf.findAll");
		for( HWConf hwconf : (List<HWConf>)  query.getResultList() ) {
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : hwconf.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					String key = String.format("%04d-%s",software.getWeigth(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			if( !keys.isEmpty() ) {
				List<String> roomSls = new ArrayList<String>();
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					roomSls.add(softwareMap.get(key)+":");
					roomSls.add("  - pkg:");
					roomSls.add("    - installed:");
				}
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_hwconf_" + hwconf.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, roomSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + hwconf.getName() + ":");
				topSls.add("    - match: nodegroups");
				topSls.add("    - oss_hwconf_" + hwconf.getName());	
			}
		}

		Path SALT_TOP   = Paths.get("/srv/salt/top.sls");
		try {
			Files.write(SALT_TOP, topSls );
		} catch( IOException e ) { 
			e.printStackTrace();
		}
		this.systemctl("restart", "salt-master");
		return new Response(this.getSession(),"OK","SoftwareState was saved succesfully"); 
	}
}