package de.openschoolserver.dao;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the Devices database table.
 * 
 */
@Entity
@Table(name="Devices")
@NamedQueries( {
	@NamedQuery(name="Device.findAll", query="SELECT d FROM Device d"),
	@NamedQuery(name="Device.getDeviceByType", query="SELECT d FROM Device where deviceType = :type"),
	@NamedQuery(name="Device.getByIP",   query="SELECT d FROM Device where ip = :IP OR wlanip = :IP"),
	@NamedQuery(name="Device.getByMAC",  query="SELECT d FROM Device where mac = :MAC OR wlanmac = :MAC"),
	@NamedQuery(name="Device.getByName", query="SELECT d FROM Device where name = :name")
})
public class Device implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private int id;

	private String name;

	private int column;

	@Column(name="IP")
	private String ip;

	@Column(name="MAC")
	private String mac;

	@Column(name="WLANIP")
	private String wlanip;

	@Column(name="WLANMAC")
	private String wlanmac;

	private int row;

	private String deviceType;

	//bi-directional many-to-many association to Device
	@ManyToMany
	@JoinTable(
		name="AvailablePrinters"
		, joinColumns={
			@JoinColumn(name="device_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="printer_id")
			}
		)
	private List<Device> availablePrinters;

	//bi-directional many-to-many association to Device
	@ManyToMany(mappedBy="availablePrinters")
	private List<Device> availableForDevices;

	//bi-directional many-to-many association to Device
	@ManyToMany
	@JoinTable(
		name="DefaultPrinters"
		, joinColumns={
			@JoinColumn(name="device_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="printer_id")
			}
		)
	private List<Device> defaultPrinter;

	//bi-directional many-to-many association to Device
	@ManyToMany(mappedBy="defaultPrinter")
	private List<Device> defaultForDevices;

	//bi-directional many-to-one association to HWConf
	@ManyToOne
	private HWConf hwconf;

	//bi-directional many-to-one association to Room
	@ManyToOne
	private Room room;

	//bi-directional many-to-one association to User
	@ManyToOne
	private User owner;

	//bi-directional many-to-many association to Room
	@ManyToMany(mappedBy="availablePrinters")
	private List<Room> availableInRooms;

	//bi-directional many-to-many association to Room
	@ManyToMany(mappedBy="defaultPrinter")
	private List<Room> defaultInRooms;

	//bi-directional many-to-one association to TestUser
	@OneToMany(mappedBy="device")
	private List<TestUser> testUsers;

	//bi-directional many-to-many association to User
	@ManyToMany(mappedBy="loggedOn")
	private List<User> loggedIn;

	public Device() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getColumn() {
		return this.column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public String getIp() {
		return this.ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMac() {
		return this.mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getWlanIp() {
		return this.wlanip;
	}

	public void setWlanIp(String wlanip) {
		this.wlanip = wlanip;
	}

	public String getWlanMac() {
		return this.wlanmac;
	}

	public void setWlanMac(String wlanmac) {
		this.wlanmac = wlanmac;
	}

	public int getRow() {
		return this.row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public String getDeviceType() {
		return this.deviceType;
	}

	public void setDeviceType(String devicetype) {
		this.deviceType = devicetype;
	}

	public List<Device> getAvailablePrinters() {
		return this.availablePrinters;
	}

	public void setAvailablePrinters(List<Device> availablePrinters) {
		this.availablePrinters = availablePrinters;
	}

	public List<Device> getAvailableForDevices() {
		return this.availableForDevices;
	}

	public void setAvailableForDevices(List<Device> availableForDevices) {
		this.availableForDevices = availableForDevices;
	}

	public List<Device> getDefaultPrinter() {
		return this.defaultPrinter;
	}

	public void setDefaultPrinter(List<Device> defaultPrinter) {
		this.defaultPrinter = defaultPrinter;
	}

	public List<Device> getDefaultForDevices() {
		return this.defaultForDevices;
	}

	public void setDefaultForDevices(List<Device> defaultForDevices) {
		this.defaultForDevices = defaultForDevices;
	}

	public HWConf getHwconf() {
		return this.hwconf;
	}

	public void setHwconf(HWConf hwconf) {
		this.hwconf = hwconf;
	}

	public Room getRoom() {
		return this.room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public User getOwner() {
		return this.owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public List<Room> getAvailableInRooms() {
		return this.availableInRooms;
	}

	public void setAvailableInRooms(List<Room> availableInRooms) {
		this.availableInRooms = availableInRooms;
	}

	public List<Room> getDefaultInRooms() {
		return this.defaultInRooms;
	}

	public void setDefaultInRooms(List<Room> defaultInRooms) {
		this.defaultInRooms = defaultInRooms;
	}

	public List<TestUser> getTestUsers() {
		return this.testUsers;
	}

	public void setTestUsers(List<TestUser> testUsers) {
		this.testUsers = testUsers;
	}

	public TestUser addTestUser(TestUser testUser) {
		getTestUsers().add(testUser);
		testUser.setDevice(this);

		return testUser;
	}

	public TestUser removeTestUser(TestUser testUser) {
		getTestUsers().remove(testUser);
		testUser.setDevice(null);

		return testUser;
	}

	public List<User> getLoggedIn() {
		return this.loggedIn;
	}

	public void setLoggedIn(List<User> loggedIn) {
		this.loggedIn = loggedIn;
	}

}
