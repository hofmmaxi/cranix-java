/* (c) 2017 Péter Varkoly <peter@varkoly.de> - all rights reserved */
package de.openschoolserver.dao;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the Tests database table.
 * 
 */
@Entity
@Table(name="Tests")
@NamedQuery(name="Test.findAll", query="SELECT t FROM Test t")
public class Test implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private long id;

	private String currentStep;

	@Convert(converter=BooleanToStringConverter.class)
	private Boolean direct;

	@Convert(converter=BooleanToStringConverter.class)
	private Boolean proxy;

	@Convert(converter=BooleanToStringConverter.class)
	private Boolean portal;
	
	@Convert(converter=BooleanToStringConverter.class)
	private Boolean login;

	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;

	private String testDir;

	private String testName;

	//bi-directional many-to-one association to TestFile
	@OneToMany(mappedBy="test")
	private List<TestFile> testFiles;

	//bi-directional many-to-one association to TestUser
	@OneToMany(mappedBy="test")
	private List<TestUser> testUsers;

	//bi-directional many-to-one association to Room
	@ManyToOne
	private Room room;

	//bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name="teacher_id")
	private User user;

	public Test() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCurrentStep() {
		return this.currentStep;
	}

	public void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
	}

	public Boolean getDirect() {
		return this.direct;
	}

	public void setDirect(Boolean direct) {
		this.direct = direct;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Boolean getProxy() {
		return this.proxy;
	}

	public void setProxy(Boolean proxy) {
		this.proxy = proxy;
	}

	public Boolean getPortal() {
		return this.portal;
	}

	public void setPortal(Boolean portal) {
		this.portal = portal;
	}
	
	public Date getStartTime() {
		return this.startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public String getTestDir() {
		return this.testDir;
	}

	public void setTestDir(String testDir) {
		this.testDir = testDir;
	}

	public String getTestName() {
		return this.testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public Boolean getLogin() {
		return this.login;
	}

	public void setLogin(Boolean login) {
		this.login = login;
	}

	public List<TestFile> getTestFiles() {
		return this.testFiles;
	}

	public void setTestFiles(List<TestFile> testFiles) {
		this.testFiles = testFiles;
	}

	public TestFile addTestFile(TestFile testFile) {
		getTestFiles().add(testFile);
		testFile.setTest(this);

		return testFile;
	}

	public TestFile removeTestFile(TestFile testFile) {
		getTestFiles().remove(testFile);
		testFile.setTest(null);

		return testFile;
	}

	public List<TestUser> getTestUsers() {
		return this.testUsers;
	}

	public void setTestUsers(List<TestUser> testUsers) {
		this.testUsers = testUsers;
	}

	public TestUser addTestUser(TestUser testUser) {
		getTestUsers().add(testUser);
		testUser.setTest(this);

		return testUser;
	}

	public TestUser removeTestUser(TestUser testUser) {
		getTestUsers().remove(testUser);
		testUser.setTest(null);

		return testUser;
	}

	public Room getRoom() {
		return this.room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
