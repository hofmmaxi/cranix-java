package de.openschoolserver.dao;

import de.openschoolserver.dao.controller.*;
import de.openschoolserver.dao.tools.*;
import java.util.regex.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Printer {
	
	/*
	 * Variables required for creating a printer
	 */
	private Long      id;
	private String    name;
	private String    manufacturer;
	private String    model;
	private String    mac;
	private Long      roomId;
	private boolean   windowsDriver;
	
	/*
	 * State variables
	 */
	private String    state;
	private boolean   acceptingJobs;
	private int       activeJobs;

	@JsonIgnore
	private Device    device;	

	public Printer() {
	}

	public Printer(String name, Session session) {
		this.name = name;
		DeviceController deviceController = new DeviceController(session);
		this.device = deviceController.getByName(name);
		this.device = deviceController.getByName(name);
		String[] program = new String[3];
		StringBuffer reply  = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		program[0] = "/usr/bin/ipptool";
		program[1] = "ipp://localhost/printers/" +name;
		program[2] = "get-printers.test";
		OSSShellTools.exec(program, reply, stderr, null);
		String[] lines = reply.toString().split(deviceController.getNl());
		Pattern pattern = Pattern.compile("\\S+");
		if( lines.length > 2) {
			Matcher matcher = pattern.matcher(lines[2]);
			if( matcher.find() ) {
				this.state = matcher.group(0);
			}
			if( matcher.find() ) {
				this.acceptingJobs = matcher.group(1).equals("true");
			}
		}
		program[2] = "get-jobs.test";
		OSSShellTools.exec(program, reply, stderr, null);
		lines = reply.toString().split(deviceController.getNl());
		this.activeJobs = lines.length-2;
		this.windowsDriver = false;
	}
	
	public Printer(String name, DeviceController deviceController) {
		this.name = name;
		this.device = deviceController.getByName(name);
		if( device != null ) {
			this.setId(device.getId());
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public boolean isAcceptingJobs() {
		return acceptingJobs;
	}

	public void setAcceptingJobs(boolean acceptingJobs) {
		this.acceptingJobs = acceptingJobs;
	}

	public int getActiveJobs() {
		return activeJobs;
	}

	public void setActiveJobs(int activeJobs) {
		this.activeJobs = activeJobs;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public Long getRoomId() {
		return roomId;
	}

	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}

	public boolean isWindowsDriver() {
		return windowsDriver;
	}

	public void setWindowsDriver(boolean windowsDriver) {
		this.windowsDriver = windowsDriver;
	}


}