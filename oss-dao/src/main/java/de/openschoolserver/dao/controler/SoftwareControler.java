package de.openschoolserver.dao.controler;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.openschoolserver.dao.*;

@SuppressWarnings( "unchecked" )
public class SoftwareControler extends Controler {
	
	Logger logger           = LoggerFactory.getLogger(CloneToolController.class);
	private static String SALT_PACKAGE_DIR = "/srv/salt/packages";

	public SoftwareControler(Session session) {
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

	public List<Software> search(String search) {
		EntityManager em = getEntityManager();
		try {
			Query query = em.createNamedQuery("SoftwareStatus.search").setParameter("search",search);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		} finally {
			em.close();
		}
	}

	public Response add(Software software) {
		EntityManager em = getEntityManager();
		//TODO it may be too simple
		try {
			em.getTransaction().begin();
			em.persist(software);
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Software was created succesfully");
	}

	public Response modify(Software software) {
		EntityManager em = getEntityManager();
		//TODO it may be too simple
		try {
			em.getTransaction().begin();
			em.merge(software);
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Software was created succesfully");
	}
	
	public Response delete(Long softwareId) {
		EntityManager em = getEntityManager();
		Software software = this.getById(softwareId);
		try {
			em.getTransaction().begin();
			if( !em.contains(software)) {
				software = em.merge(software);
			}
			em.remove(software);
			em.getTransaction().commit();
			em.getEntityManagerFactory().getCache().evictAll();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","Software was deleted succesfully");
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

	public Software getByName(String name) {
		EntityManager em = getEntityManager();
		Query query = em.createNamedQuery("Software.getByName").setParameter("name", name);
		return (Software) query.getResultList().get(0);
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
		try {
			Software s = em.find(Software.class, softwareId);
			Category c = em.find(Category.class, categoryId);
			s.getCategories().add(c);
			c.getSoftwares().add(s);
			em.getTransaction().begin();
			em.merge(s);
			em.merge(c);
			em.getTransaction().commit();
		} catch (Exception e) {
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","SoftwareState was added to category succesfully");
	}
	
	public Response removeSoftwareFromCategory(Long softwareId,Long categoryId){
		EntityManager em = getEntityManager();
		try {
			Software s = em.find(Software.class, softwareId);
			Category c = em.find(Category.class, categoryId);
			s.getCategories().remove(c);
			c.getSoftwares().remove(s);
			s.getRemovedFromCategories().add(c);
			c.getRemovedSoftwares().add(s);
			em.getTransaction().begin();
			em.merge(s);
			em.merge(c);
			em.getTransaction().commit();
			for(Room r : c.getRooms() ) {
				for( Device d : r.getDevices() ) {
					this.modifySoftwareStatusOnDevice(d,s,"","deinstallation_scheduled");
				}
			}
		} catch (Exception e) {
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","SoftwareState was added to category succesfully");
	}
	
	/*
	 * Add a license to a software
	 */
	public Response addLicenseToSoftware(SoftwareLicense softwareLicense, Long softwareId ) {
		EntityManager em = getEntityManager();
		Software software = this.getById(softwareId);
		try {
			//TODO save the file.
			em.getTransaction().begin();
			software.getSoftwareLicenses().add(softwareLicense);
			em.getTransaction().commit();
		} catch (Exception e) {
			return new Response(this.getSession(),"ERROR",e.getMessage());
		} finally {
			em.close();
		}
		return new Response(this.getSession(),"OK","License was added to the software succesfully");
	}
	
	/*
	 *  Add Licenses to a HWConf 
	 */
	public Response addSoftwareLicenseToHWConf(Software software, HWConf hwconf) {
		return this.addSoftwareLicenseToDevices(software, hwconf.getDevices());
	}
	
	/*
	 *  Add Licenses to a Room
	 */
	public Response addSoftwareLicenseToRoom(Software software, Room room) {
		return this.addSoftwareLicenseToDevices(software, room.getDevices());
	}
	
	/*
	 * Add Licenses to devices
	 */
	public Response addSoftwareLicenseToDevices(Software software, List<Device> devices ){
		EntityManager em = getEntityManager();
		SoftwareLicense softwareLicense;
 		List<String> failedDevices = new ArrayList<String>();
 		for( Device device : devices ) {
 			for( SoftwareLicense myLicense : device.getSoftwareLicences() ) {
 				if( myLicense.getSoftware().equals(software) ){
 					continue;
 				}
 			}
 			softwareLicense = this.getNextFreeLicenseId(software);
 			if( softwareLicense == null) {
 				failedDevices.add(device.getName());
 			} else {
 				try {
 					em.getTransaction().begin();
 					device.getSoftwareLicences().add(softwareLicense);
 					softwareLicense.getDevices().add(device);
 					em.getTransaction().commit();
 				} catch (Exception e) {
 					logger.error(e.getMessage());
 					em.close();
 					return new Response(this.getSession(),"ERROR",e.getMessage());
 				}
 			}
 		}
 		em.close();
 		if(failedDevices.isEmpty() ) {
 			return new Response(this.getSession(),"OK","License was added to the devices succesfully");
 		}
 		return new Response(this.getSession(),"ERROR","License could not be added to the following devices" + String.join(", ", failedDevices));
	}
	
	/*
	 * Return the next free license
	 */
	private  SoftwareLicense getNextFreeLicenseId(Software software) {
		for( SoftwareLicense softwareLicense : software.getSoftwareLicenses() ) {
			if( softwareLicense.getCount() > softwareLicense.getDevices().size() ) {
				return softwareLicense;
			}
		}
		return null;
	}

	/*
	 * Sets the software status on a device to a given version and remove the other status.
	 */
	public void setSoftwareStatusOnDevice(Device d, Software s,  String version, String status) {
		EntityManager em = getEntityManager();
		List<SoftwareVersion> lsv;
		Query query= em.createNamedQuery("SoftwareStatus.get")
				.setParameter("SOFTWARE", s.getId())
				.setParameter("DEVICE", d.getId());
		
		List<SoftwareStatus> lss = query.getResultList();
		try {
			em.getTransaction().begin();
			if( !lss.isEmpty()) {
				for( SoftwareStatus ss : lss ) {
					if( ss.getSoftwareVersion().getVersion().equals(version) ) {
						ss.setStatus(status);
						em.merge(ss);
					} else {
						em.remove(ss);
					}
				}
			} else {
				query = em.createNamedQuery("SoftwareVersion.getBySDoftware")
						.setParameter("SOFTWARE", s.getId());
				lsv = query.getResultList();
				if( lsv.isEmpty() ) {
					SoftwareVersion sv = new SoftwareVersion(s,"0.0");
					em.persist(sv);
					SoftwareStatus ss = new SoftwareStatus(d,sv,status);
					em.persist(ss);
				} else {
					for( SoftwareVersion sv : lsv ) {
						SoftwareStatus ss = new SoftwareStatus(d,sv,status);
						em.persist(ss);
					}
				}
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			em.close();
		}
	}

	/*
	 * Modify the software status on a device. If there is no status nothing will be happenend.
	 */
	public void modifySoftwareStatusOnDevice(Device d, Software s,  String version, String status) {
		EntityManager em = getEntityManager();
		Query query;
		if( version.isEmpty() ) {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId());
		} else {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId())
					.setParameter("VERSION", version);
		}
		List<SoftwareStatus> lss = query.getResultList();
		try {
			em.getTransaction().begin();
			for( SoftwareStatus ss : lss ) {
					ss.setStatus(status);
					em.merge(ss);
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			em.close();
		}
	}

	/*
	 * Remove the software status from a device.
	 */
	public void removeSoftwareStatusOnDevice(Device d, Software s,  String version) {
		EntityManager em = getEntityManager();
		Query query;
		if( version.isEmpty() ) {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId());
		} else {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId())
					.setParameter("VERSION", version);
		}
		List<SoftwareStatus> lss = query.getResultList();
		try {
			em.getTransaction().begin();
			for( SoftwareStatus ss : lss ) {
					em.remove(ss);
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			em.close();
		}
	}

	/*
	 * Reads the software status to a given version of a software on a device.
	 */
	public String getSoftwareStatusOnDevice(Device d, Software s,  String version) {
		EntityManager em = getEntityManager();
		Query query;
		if( version.isEmpty() ) {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId());
		} else {
			query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId())
					.setParameter("VERSION", version);
		}
		List<SoftwareStatus> lss = query.getResultList();
		for( SoftwareStatus ss : lss ) {
			return ss.getStatus();
		}
		em.close();
		return "";
	}

	/*
	 * Checks if there is a software status to a given version of a software on a device.
	 */
	public boolean checkSoftwareStatusOnDevice(Device d, Software s,  String version) {
		EntityManager em = getEntityManager();
		Query query = em.createNamedQuery("SoftwareStatus.get")
					.setParameter("SOFTWARE", s.getId())
					.setParameter("DEVICE", d.getId())
					.setParameter("VERSION", version);
		return ! query.getResultList().isEmpty();
	}

	/*
	 * Save the software status what shall be installed to host sls files.
	 */
	public Response applySoftwareStateToHosts(){
		EntityManager em = getEntityManager();
		RoomControler   roomControler   = new RoomControler(this.session);
		DeviceControler deviceControler = new DeviceControler(this.session);
		Map<Device,List<String>>   softwaresToInstall = new HashMap<>();
		Map<Device,List<Software>> softwaresToRemove  = new HashMap<>();
		String key;

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
		for( Device device : deviceControler.getAll() ) {
			List<String>   toInstall = new ArrayList<String>();
			List<Software> toRemove  = new ArrayList<Software>();
			for( Category category : device.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					toRemove.add(software);
				}
			}
			for( Category category : device.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					toRemove.remove(software);
					for( Software requirement : software.getRequirements() ) {
						toInstall.add(String.format("%04d-%s",requirement.getWeight(),requirement.getName()));
					}
					toInstall.add(String.format("%04d-%s",software.getWeight(),software.getName()));
				}
			}
			softwaresToInstall.put(device, toInstall);
			softwaresToRemove.put(device, toRemove);
		}

		//Create the room state files
		for( Room room : roomControler.getAll() ) {
			List<Software> toRemove  = new ArrayList<Software>();
			
			for( Category category : room.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					toRemove.add(software);
				}
			}
			for( Category category : room.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					toRemove.remove(software);
					for( Software requirement : software.getRequirements() ) {
						key = String.format("%04d-%s",requirement.getWeight(),requirement.getName());
						for( Device device : room.getDevices() ) {
							if( ! softwaresToInstall.get(device).contains(key) ) {
								softwaresToInstall.get(device).add(key);
							}
						}
					}
					key = String.format("%04d-%s",software.getWeight(),software.getName());
					for( Device device : room.getDevices() ) {
						if( ! softwaresToInstall.get(device).contains(key) ) {
							softwaresToInstall.get(device).add(key);
						}
					}
				}
			}
			for( Device device : room.getDevices() ) {
				for( Software software : toRemove ) {
					if( ! softwaresToRemove.get(device).contains(software) ) {
						softwaresToRemove.get(device).add(software);
					}
				}
			}
		}

		//Create the hwconf state files
		Query query = em.createNamedQuery("HWConf.findAll");
		for( HWConf hwconf : (List<HWConf>)  query.getResultList() ) {
			//List of software to be removed
			List<Software> toRemove  = new ArrayList<Software>();
			for( Category category : hwconf.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					toRemove.add(software);
				}
			}
			for( Category category : hwconf.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					toRemove.remove(software);
					for( Software requirement : software.getRequirements() ) {
						key = String.format("%04d-%s",requirement.getWeight(),requirement.getName());
						for( Device device : hwconf.getDevices() ) {
							if( ! softwaresToInstall.get(device).contains(key) ) {
								softwaresToInstall.get(device).add(key);
							}
						}
					}
					key = String.format("%04d-%s",software.getWeight(),software.getName());
					for( Device device : hwconf.getDevices() ) {
						if( ! softwaresToInstall.get(device).contains(key) ) {
							softwaresToInstall.get(device).add(key);
						}
					}
				}
			}
			for( Device device : hwconf.getDevices() ) {
				for( Software software : toRemove ) {
					if( ! softwaresToRemove.get(device).contains(software) ) {
						softwaresToRemove.get(device).add(software);
					}
				}
			}
		}

		//Write the hosts sls files
		for( Device device : deviceControler.getAll() ) {
			List<String> deviceSls = new ArrayList<String>();
			//Remove first the softwares.
			for( Software software : softwaresToRemove.get(device) ) {
				deviceSls.add(software.getName()+":");
				deviceSls.add("  - pkg:");
				deviceSls.add("    - removed:");
				if( this.checkSoftwareStatusOnDevice(device, software, "installed") ){
					this.setSoftwareStatusOnDevice(device, software, "", "deinstallation_scheduled");
				}
			}
			softwaresToInstall.get(device).sort((String s1, String s2) -> { return s2.compareTo(s1); });
			for( String softwareKey :  softwaresToInstall.get(device) ) {
				
				String softwareName = softwareKey.substring(4);
				StringBuilder filePath = new StringBuilder(SALT_PACKAGE_DIR);
				filePath.append(softwareName).append(".sls");
				File file = new File(filePath.toString());
				if( file.exists() ) {
					//TODO
				} else {
					deviceSls.add(softwareName+":");
					deviceSls.add("  - pkg:");
					deviceSls.add("    - installed:");					
				}
				Software software = this.getByName(softwareName);
				if(! this.checkSoftwareStatusOnDevice(device, software, "installed")){
					this.setSoftwareStatusOnDevice(device, software, "", "installation_scheduled");
				}
			}
			if( deviceSls.size() > 0) {
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_device_" + device.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, deviceSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + device.getName() + ":");
				topSls.add("    - oss_device_" + device.getName());
			}
		}
		if( topSls.size() > 0 ) {
			Path SALT_TOP   = Paths.get("/srv/salt/top.sls");
			try {
				Files.write(SALT_TOP, topSls );
			} catch( IOException e ) { 
				e.printStackTrace();
			}
			this.systemctl("restart", "salt-master");
		}
		return new Response(this.getSession(),"OK","SoftwareState was saved succesfully"); 
	}

	/*
	 * Save the software status what shall be installed where to host room and hwconf sls files
	 */
	public Response applySoftwareStateToNodeGroups(){
		EntityManager em = getEntityManager();
		RoomControler   roomControler   = new RoomControler(this.session);
		DeviceControler deviceControler = new DeviceControler(this.session);
		
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
		for( Device device : deviceControler.getAll() ) {
			//List of software to be removed
			List<String> softwaresToDeinstall = new ArrayList<String>();
			for( Category category : device.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					softwaresToDeinstall.add(software.getName());
				}
			}
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : device.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					softwaresToDeinstall.remove(software);
					String key = String.format("%04d-%s",software.getWeight(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			List<String> deviceSls = new ArrayList<String>();
			if( !keys.isEmpty() ) {
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					String softwareName = softwareMap.get(key);
					Software software = this.getByName(softwareName);
					File file = new File(new StringBuilder(SALT_PACKAGE_DIR).append(softwareName).append(".sls").toString());
					if( file.exists() ) {
						//TODO
					} else {
						deviceSls.add(softwareName+":");
						deviceSls.add("  - pkg:");
						deviceSls.add("    - installed:");					
					}
					if(! this.checkSoftwareStatusOnDevice(device, software, "installed")){
						this.setSoftwareStatusOnDevice(device, software, "", "installation_scheduled");
					}
				}
			}
			for( String key : softwaresToDeinstall ) {
				deviceSls.add(key+":");
				deviceSls.add("  - pkg:");
				deviceSls.add("    - removed:");
				Software software = this.getByName(key);
				if( this.checkSoftwareStatusOnDevice(device, software, "installed") ){
					this.setSoftwareStatusOnDevice(device, software, "", "deinstallation_scheduled");
				}
		    }
			if( deviceSls.size() > 0) {
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_device_" + device.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, deviceSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + device.getName() + ":");
				topSls.add("    - oss_device_" + device.getName());
			}
		}

		//Create the room state files
		for( Room room : roomControler.getAll() ) {
			//List of software to be removed
			List<String> softwaresToDeinstall = new ArrayList<String>();
			for( Category category : room.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					softwaresToDeinstall.add(software.getName());
				}
			}
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : room.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					softwaresToDeinstall.remove(software);
					String key = String.format("%04d-%s",software.getWeight(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			List<String> roomSls = new ArrayList<String>();
			if( !keys.isEmpty() ) {
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					String softwareName = softwareMap.get(key);
					File file = new File(new StringBuilder(SALT_PACKAGE_DIR).append(softwareName).append(".sls").toString());
					if( file.exists() ) {
						//TODO
					} else {
						roomSls.add(softwareName+":");
						roomSls.add("  - pkg:");
						roomSls.add("    - installed:");					
					}
				}
			}
			for( String key : softwaresToDeinstall ) {
				roomSls.add(key+":");
				roomSls.add("  - pkg:");
				roomSls.add("    - removed:");
		    }
			if( roomSls.size() > 0) {
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
			//List of software to be removed
			List<String> softwaresToDeinstall = new ArrayList<String>();
			for( Category category : hwconf.getCategories() ) {
				for( Software software : category.getRemovedSoftwares() ) {
					softwaresToDeinstall.add(software.getName());
				}
			}
			//List of software to be installed
			Map<String,String> softwareMap = new HashMap<>();
			List<String> keys = new ArrayList<String>();
			for( Category category : hwconf.getCategories() ) {
				for( Software software : category.getSoftwares() ) {
					softwaresToDeinstall.remove(software);
					String key = String.format("%04d-%s",software.getWeight(),software.getName());
					softwareMap.put(key,software.getName());
					keys.add(key);
				}
			}
			List<String> hwconfSls = new ArrayList<String>();
			if( !keys.isEmpty() ) {
				keys.sort((String s1, String s2) -> { return s2.compareTo(s1); });
				for( String key : keys ){
					String softwareName = softwareMap.get(key);
					File file = new File(new StringBuilder(SALT_PACKAGE_DIR).append(softwareName).append(".sls").toString());
					if( file.exists() ) {
						//TODO
					} else {
						hwconfSls.add(softwareName+":");
						hwconfSls.add("  - pkg:");
						hwconfSls.add("    - installed:");					
					}
				}
			}
			for( String key : softwaresToDeinstall ) {
					hwconfSls.add(key+":");
					hwconfSls.add("  - pkg:");
					hwconfSls.add("    - removed:");
			}
			if( hwconfSls.size() > 0) {
				Path SALT_ROOM   = Paths.get("/srv/salt/oss_hwconf_" + hwconf.getName() + ".sls");
				try {
					Files.write(SALT_ROOM, hwconfSls );
				} catch( IOException e ) { 
					e.printStackTrace();
				}
				topSls.add("  - " + hwconf.getName() + ":");
				topSls.add("    - match: nodegroups");
				topSls.add("    - oss_hwconf_" + hwconf.getName());
			}
		}

		if( topSls.size() > 0 ) {
			Path SALT_TOP   = Paths.get("/srv/salt/top.sls");
			try {
				Files.write(SALT_TOP, topSls );
			} catch( IOException e ) { 
				e.printStackTrace();
			}
			this.systemctl("restart", "salt-master");
		}
		return new Response(this.getSession(),"OK","SoftwareState was saved succesfully"); 
	}
}
