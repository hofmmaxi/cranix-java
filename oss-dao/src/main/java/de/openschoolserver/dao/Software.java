package de.openschoolserver.dao;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the Software database table.
 * 
 */
@Entity
@NamedQuery(name="Software.findAll", query="SELECT s FROM Software s")
public class Software implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="SOFTWARE_ID_GENERATOR" )
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SOFTWARE_ID_GENERATOR")
	private long id;

	private String description;

	@Convert(converter=BooleanToStringConverter.class)
	private Boolean manuell;

	private String name;

	private Integer weigth;
	
	//bi-directional many-to-one association to SoftwareLicens
	@OneToMany(mappedBy="software")
	private List<SoftwareLicense> softwareLicenses;

	//bi-directional many-to-one association to SoftwareVersion
	@OneToMany(mappedBy="software")
	private List<SoftwareVersion> softwareVersions;

	public Software() {
		this.manuell = false;
	}

	@Override
    public boolean equals(Object obj) {
	      if (obj instanceof Software && obj !=null) {
	                  return getId() == ((Software)obj).getId();
	      }
	      return super.equals(obj);
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Integer getWeigth() {
		return this.weigth;
	}

	public void setWeigth(Integer weigth) {
		this.weigth = weigth;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getManuell() {
		return this.manuell;
	}

	public void setManuell(Boolean manuell) {
		this.manuell = manuell;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SoftwareLicense> getSoftwareLicenses() {
		return this.softwareLicenses;
	}

	public void setSoftwareLicenses(List<SoftwareLicense> softwareLicenses) {
		this.softwareLicenses = softwareLicenses;
	}

	public SoftwareLicense addSoftwareLicens(SoftwareLicense softwareLicens) {
		getSoftwareLicenses().add(softwareLicens);
		softwareLicens.setSoftware(this);

		return softwareLicens;
	}

	public SoftwareLicense removeSoftwareLicens(SoftwareLicense softwareLicens) {
		getSoftwareLicenses().remove(softwareLicens);
		softwareLicens.setSoftware(null);

		return softwareLicens;
	}

	public List<SoftwareVersion> getSoftwareVersions() {
		return this.softwareVersions;
	}

	public void setSoftwareVersions(List<SoftwareVersion> softwareVersions) {
		this.softwareVersions = softwareVersions;
	}

	public SoftwareVersion addSoftwareVersion(SoftwareVersion softwareVersion) {
		getSoftwareVersions().add(softwareVersion);
		softwareVersion.setSoftware(this);

		return softwareVersion;
	}

	public SoftwareVersion removeSoftwareVersion(SoftwareVersion softwareVersion) {
		getSoftwareVersions().remove(softwareVersion);
		softwareVersion.setSoftware(null);

		return softwareVersion;
	}

}