/* (c) 2017 Péter Varkoly <peter@varkoly.de> - all rights reserved */
package de.openschoolserver.dao;

import java.io.Serializable;

import javax.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * The persistent class for the HWConfs database table.
 * 
 */
@Entity
@Table(name="HWConfs")
@NamedQueries({
	@NamedQuery(name="HWConf.findAll", query="SELECT h FROM HWConf h"),
	@NamedQuery(name="HWConf.getByName", query="SELECT h FROM HWConf h WHERE h.name = :name")
})
@SequenceGenerator(name="seq", initialValue=1, allocationSize=100)
public class HWConf implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="seq")
	private long id;

	private String description;

	private String name;

	private String deviceType;

	//bi-directional many-to-one association to Device
	@OneToMany(mappedBy="hwconf")
	@JsonIgnore
	private List<Device> devices;

	//bi-directional many-to-one association to Partition
	@OneToMany(mappedBy="hwconf", cascade={ CascadeType.REMOVE,CascadeType.PERSIST })
	private List<Partition> partitions;

	//bi-directional many-to-one association to Room
	@OneToMany(mappedBy="hwconf")
	@JsonIgnore
	private List<Room> rooms;
	
	//bi-directional many-to-many association to Category
	@ManyToMany(mappedBy="hwconfs",cascade ={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	@JsonIgnore
	private List<Category> categories;
	
    //bi-directional many-to-one association to User
	@ManyToOne
	@JsonIgnore
	private User creator;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HWConf && obj !=null) {
            return getId() == ((HWConf)obj).getId();
        }
        return super.equals(obj);
    }

	public HWConf() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}



	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public List<Device> getDevices() {
		return this.devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	public Device addDevice(Device device) {
		getDevices().add(device);
		device.setHwconf(this);

		return device;
	}

	public Device removeDevice(Device device) {
		getDevices().remove(device);
		device.setHwconf(null);

		return device;
	}

	public List<Partition> getPartitions() {
		return this.partitions;
	}

	public void setPartitions(List<Partition> partitions) {
		this.partitions = partitions;
	}

	public Partition addPartition(Partition partition) {
		getPartitions().add(partition);
		partition.setHwconf(this);

		return partition;
	}

	public Partition removePartition(Partition partition) {
		getPartitions().remove(partition);
		partition.setHwconf(null);

		return partition;
	}
	
	public List<Room> getRooms() {
		return this.rooms;
	}

	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}

	public Room addRoom(Room room) {
		getRooms().add(room);
		room.setHwconf(this);
		return room;
	}

	public Room removeRoom(Room room) {
		getRooms().remove(room);
		room.setHwconf(null);
		return room;
	}

    public List<Category> getCategories() {
        return this.categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

}
