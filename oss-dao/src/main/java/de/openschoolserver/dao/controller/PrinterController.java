package de.openschoolserver.dao.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.openschoolserver.dao.Device;
import de.openschoolserver.dao.HWConf;
import de.openschoolserver.dao.OssResponse;
import de.openschoolserver.dao.Printer;
import de.openschoolserver.dao.Session;
import de.openschoolserver.dao.tools.OSSShellTools;

public class PrinterController extends Controller {
	private Path DRIVERS   = Paths.get("/usr/share/oss/templates/drivers.txt");
	public PrinterController(Session session,EntityManager em) {
		super(session,em);
		// TODO Auto-generated constructor stub
	}

	public String getModel(String name){
		try {
			Pattern pattern = Pattern.compile(".NickName: \"(.*)\"");
			logger.debug("getModel of:" + name);
			for( String line : Files.readAllLines(Paths.get("/etc/cups/ppd/" + name + ".ppd")) ) {
				Matcher matcher = pattern.matcher(line);
				if( matcher.find() ) {
					return matcher.group(1);
				}
			}
		} catch (IOException e) {
			logger.debug("getModel of: " + name + " ppd file not found");
		}
		return "";
	}

	/**
	 * Find a printer by id
	 * @param printerId
	 * @return
	 */
	public Printer getById(long printerId) {
		try {
			Printer printer = this.em.find(Printer.class, printerId);
			if( printer != null ) {
				if( printer.getDevice() != null ) {
					printer.setDeviceName(printer.getDevice().getName());
				}
				printer.setModel(getModel(printer.getName()));
			}
			return printer;
		} catch (Exception e) {
			return null;
		} finally {
		}
	}

	/**
	 * Find a printer by the name
	 * @param name
	 * @return
	 */
	public Printer getByName(String name) {
		try {
			Query query = this.em.createNamedQuery("Printer.getByName");
			query.setParameter("name", name);
			Printer printer = (Printer) query.getSingleResult();
			printer.setRoomId(printer.getDevice().getRoom().getId());
			return printer;
		} catch (Exception e) {
			logger.debug("name " + name  + " " + e.getMessage());
			return null;
		} finally {
		}
	}

	/**
	 * Delivers a list of all available printer
	 * @return
	 */
	public List<Printer> getPrinters() {
		List<Printer> printers = new ArrayList<Printer>();
		String[] program = new String[3];
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		program[0] = "/usr/bin/ipptool";
		program[1] = "ipp://localhost/printers/";
		program[2] = "get-printers.test";
		OSSShellTools.exec(program, reply, stderr, null);
		logger.debug(stderr.toString());
		String[] lines = reply.toString().split(this.getNl());
		Pattern pattern = Pattern.compile("\\S+");
		for( int i=2 ; i < lines.length; i ++) {
			logger.debug("printer:" + lines[i]);
			Matcher matcher = pattern.matcher(lines[i]);
			if( matcher.find()) {
				String  name    = matcher.group(0);
				Printer printer = getByName(name);
				if( printer == null ) {
					continue;
				}
				if( matcher.find() ) {
					printer.setState(matcher.group(0));
				}
				if( matcher.find() ) {
					printer.setAcceptingJobs(matcher.group(0).equals("true"));
				}
				if( matcher.find() ) {
					logger.debug("Found job:" + matcher.group(0));
					printer.setActiveJobs(Integer.parseInt(matcher.group(0)));
				}
				// Test if the windows driver was activated
				File file = new File("/var/lib/printserver/drivers/x64/3/"+name+".ppd");
				if( file.exists() ) {
					printer.setWindowsDriver(true);
				}
				if( printer.getDevice() != null ) {
					printer.setDeviceName(printer.getDevice().getName());
				}
				printer.setModel(getModel(name));
				printers.add(printer);
			}
		}
		return printers;
	}
	
	/**
	 * Deletes a printer found by name.
	 * @param printerId
	 * @return
	 */
	public OssResponse deletePrinter(Long printerId) {

		OssResponse ossResponse = new OssResponse(session,"OK","Printer was deleted succesfully.");
		try {
			Printer printer = this.em.find(Printer.class, printerId);
			if( printer == null ) {
				return new OssResponse(this.getSession(),"ERROR", "Can not find printer with id %s.",null,String.valueOf(printerId));
			}
			Device  printerDevice = printer.getDevice();
			String[] program    = new String[3];
			StringBuffer reply  = new StringBuffer();
			StringBuffer stderr = new StringBuffer();
			program[0] = "/usr/sbin/lpadmin";
			program[1] = "-x";
			program[2] = printer.getName();
			OSSShellTools.exec(program, reply, stderr, null);
			this.beginTransaction();
			printerDevice.getPrinterQueue().remove(printer);
			this.em.remove(printer);
			this.em.merge(printerDevice);
			this.em.getTransaction().commit();
			if( printerDevice.getPrinterQueue().isEmpty() ) {
				ossResponse = new DeviceController(this.session,this.em).delete(printerDevice, true);
			}
		} catch (Exception e) {
			logger.debug("deletePrinter :" + e.getMessage());
			return null;
		} finally {
		}
		return ossResponse;
	}

	public OssResponse activateWindowsDriver(String printerName) {
		logger.debug("Activating windows driver for: " + printerName);
		if( session.getPassword() == null || session.getPassword().isEmpty() ) {
			return new OssResponse(session,"ERROR", "The session password of the administrator is expiered. Please login into the web interface again.");
		}
		String printserver   = new RoomController(this.session,this.em).getConfigValue("PRINTSERVER");
		String[] program     = new String[7];
		StringBuffer reply   = new StringBuffer();
		StringBuffer stderr  = new StringBuffer();
		program[0] = "/usr/sbin/cupsaddsmb";
		program[1] = "-v";
		program[2] = "-H";
		program[3] = printserver;
		program[4] = "-U";
		program[5] = "Administrator%" + session.getPassword();
		program[6] = printerName;
		int success = OSSShellTools.exec(program, reply, stderr, null);
		try {
			logger.debug(new ObjectMapper().writeValueAsString(program));
		} catch (Exception e) {
			logger.debug( "{ \"ERROR\" : \"CAN NOT MAP THE OBJECT\" }" );
		}
		logger.debug("activateWindowsDriver cupsaddsmb stderr: " + stderr.toString());
		if( success != 0 ) {
			return new OssResponse(session,"ERROR", stderr.toString());
		}
		program     = new String[6];
		program[0] = "/usr/bin/rpcclient";
		program[1] = "-U";
		program[2] = "Administrator%" + session.getPassword();
		program[3] = printserver;
		program[4] = "-c";
		program[5] = "setdriver " + printerName + " " + printerName;
		logger.debug("activateWindowsDriver rpcclient: " + reply.toString());
		OSSShellTools.exec(program, reply, stderr, null);
		logger.debug("activateWindowsDriver error" + stderr.toString());
		logger.debug("activateWindowsDriver reply" + reply.toString());
		return new OssResponse(session,"OK","Windows driver was activated.");
	}

	public OssResponse addPrinter(String name, String mac, Long roomId, String model, boolean windowsDriver,
			InputStream fileInputStream, FormDataContentDisposition contentDispositionHeader) {
		logger.debug("addPrinter: " + name + "#" + mac + "#" + roomId + "#" + model +"#" + ( windowsDriver ? "yes" : "no" ) );
		//First we create a device object
		RoomController roomController = new RoomController(this.session,this.em);;
		HWConf hwconf = new CloneToolController(this.session,this.em).getByName("Printer");
		Device device = new Device();
		device.setMac(mac);
		device.setName(name.toLowerCase());
		device.setHwconfId(hwconf.getId());
		logger.debug(hwconf.getName() + "#" + hwconf.getId() );
		List<Device> devices = new ArrayList<Device>();
		devices.add(device);
		//Persist the device object
		OssResponse ossResponse = roomController.addDevices(roomId, devices);
		if( ossResponse.getCode().equals("ERROR")) {
			return ossResponse;
		}
		return addPrinterQueue(session,name,device.getId(),model,windowsDriver,fileInputStream,contentDispositionHeader);
	}

	public OssResponse addPrinterQueue(Session session, String name, Long deviceId, String model, boolean windowsDriver,
				InputStream fileInputStream, FormDataContentDisposition contentDispositionHeader) {
		
		RoomController roomController = new RoomController(this.session,this.em);;
		String deviceHostName;
		Printer printer = new Printer();
		//Create the printer object
		try {
			Device device = this.em.find(Device.class, deviceId);
			this.beginTransaction();
			
			printer.setDevice(device);
			printer.setName(name.toLowerCase());
			this.em.persist(printer);
			logger.debug("Created Printer: " + printer);
			device.getPrinterQueue().add(printer);
			this.em.merge(device);
			this.em.getTransaction().commit();
			deviceHostName = device.getName();
		} catch (Exception e){
			logger.debug("addPrinterQueue :" + e.getMessage());
			return new OssResponse(session,"ERROR",e.getMessage());
		} finally {
		}
		//Create the printer in CUPS
		String driverFile = "/usr/share/cups/model/Postscript.ppd.gz";
		if( fileInputStream != null ) {
			File file = null;
			try {
				file = File.createTempFile("oss_driverFile", name, new File("/opt/oss-java/tmp/"));
				Files.copy(fileInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return new OssResponse(session,"ERROR", e.getMessage());
			}
			driverFile = file.toPath().toString();
		} else {
			try {
				for( String line : Files.readAllLines(DRIVERS) ) {
					String[] fields = line.split("###");
					if( fields.length == 2 && fields[0].equals(model) ) {
						driverFile = fields[1];
						break;
					}
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return new OssResponse(session,"ERROR", e.getMessage());
			}
		}
		logger.debug("Add printer/usr/sbin/lpadmin -p " + name + " -P " + driverFile + " -o printer-error-policy=abort-job -o PageSize=A4 -v socket://" + name   );
		String[] program = new String[11];
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		program[0] = "/usr/sbin/lpadmin";
		program[1] = "-p";
		program[2] = name;
		program[3] = "-P";
		program[4] = driverFile;
		program[5] = "-o";
		program[6] = "printer-error-policy=abort-job";
		program[7] = "-o";
		program[8] = "PageSize=A4";
		program[9] = "-v";
		program[10]= "socket://"+ deviceHostName;
				
		OSSShellTools.exec(program, reply, stderr, null);
		logger.debug(stderr.toString());
		logger.debug(reply.toString());
		roomController.systemctl("try-restart", "samba-printserver");
		//Now we have to check if the printer is already visible in samba
		int tries = 6;
		program = new String[6];
		program[0] = "/usr/bin/rpcclient";
		program[1] = "-U";
		program[2] = "Administrator%" + session.getPassword();
		program[3] = roomController.getConfigValue("PRINTSERVER");
		program[4] = "-c";
		program[5] = "getprinter "+name;
		do {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			reply   = new StringBuffer();
			stderr  = new StringBuffer();
			OSSShellTools.exec(program, reply, stderr, null);
			logger.debug("activateWindowsDriver error" + stderr.toString());
		} while( !stderr.toString().isEmpty() && tries > -1  );
		
		if(windowsDriver) {
			OssResponse ossResponse = activateWindowsDriver(name);
			if( ossResponse.getCode().equals("ERROR")) { 
				return ossResponse;
			}
		}
		enablePrinter(name);

		return new OssResponse(
				session,"OK",
				"Printer was created succesfully.",
				printer.getId()
				);
	}

	public OssResponse enablePrinter(String printerName) {
		String[] program = new String[2];
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		program[0] = "/usr/sbin/cupsenable";
		program[1] = printerName;
		OSSShellTools.exec(program, reply, stderr, null);
		program[0] = "/usr/sbin/cupsaccept";
		program[1] = printerName;
		OSSShellTools.exec(program, reply, stderr, null);
		return new OssResponse(session,"OK","Printer was enabled succesfully.");
	}

}
