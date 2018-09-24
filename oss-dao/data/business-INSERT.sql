INSERT INTO Users VALUES(1,'Administrator','','sysadmins','Administrator','Main',NOW(),0,0,0,0,'',NULL);
INSERT INTO Users VALUES(4,'tadministration','','administration','for administration','Default profile',NOW(),0,0,0,0,'',1);
INSERT INTO Users VALUES(5,'tworkstations','','workstations','for workstations','Default profile',NOW(),0,0,0,0,'',1);
INSERT INTO Users VALUES(6,'cephalix','','internal','Administrator','Internal',NOW(),0,0,0,0,'',1);
INSERT INTO Users VALUES(7,'register','','internal','Register','Internal',NOW(),0,0,0,0,'',1);
INSERT INTO Users VALUES(8,'ossreader','','internal','Reader','Account',NOW(),0,0,0,0,'',1);
INSERT INTO Groups VALUES(1,'sysadmins','Sysadmins','primary',1);
INSERT INTO Groups VALUES(4,'administration','Administration','primary',1);
INSERT INTO Groups VALUES(5,'workstations','Workstations','primary',1);
INSERT INTO Groups VALUES(6,'templates','Templates','primary',1);
INSERT INTO GroupMember VALUES(1,1);
INSERT INTO GroupMember VALUES(4,6);
INSERT INTO GroupMember VALUES(5,6);
INSERT INTO GroupMember VALUES(4,4);
INSERT INTO GroupMember VALUES(5,5);
INSERT INTO GroupMember VALUES(6,1);
INSERT INTO HWConfs VALUES(1,"Server","","Server",1);
INSERT INTO HWConfs VALUES(2,"Printer","","Printer",1);
INSERT INTO HWConfs VALUES(3,"BYOD","Privat Devices","BYOD",1);
INSERT INTO HWConfs VALUES(4,'Win10-64-Domain','Win10 64Bit Domain Member','FatClient',1);
INSERT INTO Partitions values(1,4,'sda1','Boot','WinBoot','no','partimage',NULL,1);
INSERT INTO Partitions values(2,4,'sda2','System','Win10','Domain','partimage',NULL,1);
INSERT INTO HWConfs VALUES(5,'Win10-64-No-Join','Win10 64Bit without Domain Join','FatClient',1);
INSERT INTO Partitions values(3,5,'sda1','Boot','WinBoot','no','partimage',NULL,1);
INSERT INTO Partitions values(4,5,'sda2','System','Win10','no','partimage',NULL,1);
INSERT INTO Rooms VALUES(1,1,'SERVER_NET','Virtual room for servers','technicalRoom','no',10,10,'#SERVER_NETWORK#',#SERVER_NETMASK#,6);
INSERT INTO Rooms VALUES(2,NULL,'ANON_DHCP','Virtual room for unknown devices','technicalRoom','no',10,10,'#ANON_NETWORK#',#ANON_NETMASK#,6);
INSERT INTO Devices VALUES(1,1,1,NULL,'#SCHOOL_NETBIOSNAME#','#SCHOOL_SERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(2,1,1,NULL,'schoolserver','#SCHOOL_MAILSERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(3,1,1,NULL,'proxy','#SCHOOL_PROXY#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(4,1,1,NULL,'printserver','#SCHOOL_PRINTSERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(5,1,1,NULL,'backup','#SCHOOL_BACKUP_SERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(6,1,1,NULL,'install','#SCHOOL_SERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Devices VALUES(7,1,1,NULL,'timeserver','#SCHOOL_SERVER#',NULL,'','',0,0,'','','',0);
INSERT INTO Enumerates VALUES(NULL,'deviceType','FatClient',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','Printer',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','Router',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','Server',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','Switch',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','ThinClient',1);
INSERT INTO Enumerates VALUES(NULL,'deviceType','BYOD',1);
INSERT INTO Enumerates VALUES(NULL,'role','sysadmins',1);
INSERT INTO Enumerates VALUES(NULL,'role','administration',1);
INSERT INTO Enumerates VALUES(NULL,'role','workstations',1);
INSERT INTO Enumerates VALUES(NULL,'groupType','primary',1);
INSERT INTO Enumerates VALUES(NULL,'groupType','workgroup',1);
INSERT INTO Enumerates VALUES(NULL,'groupType','guest',1);
INSERT INTO Enumerates VALUES(NULL,'roomControl','inRoom',1);
INSERT INTO Enumerates VALUES(NULL,'roomControl','no',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','ComputerRoom',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','Library',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','Laboratory',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','WlanAccess',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','AdHocAccess',1);
INSERT INTO Enumerates VALUES(NULL,'roomType','TechnicalRoom',1);
INSERT INTO Enumerates VALUES(NULL,'accessType','DEFAULT',1);
INSERT INTO Enumerates VALUES(NULL,'accessType','FW',1);
INSERT INTO Enumerates VALUES(NULL,'accessType','ACT',1);
INSERT INTO Enumerates VALUES(NULL,'licenseType','NONE',1);
INSERT INTO Enumerates VALUES(NULL,'licenseType','FILE',1);
INSERT INTO Enumerates VALUES(NULL,'licenseType','CMD',1);
INSERT INTO Enumerates VALUES(NULL,'categoryType','software',1);
INSERT INTO Enumerates VALUES(NULL,'categoryType','virtualRoom',1);
INSERT INTO Enumerates VALUES(NULL,'network','#SCHOOL_NETWORK#/#SCHOOL_NETMASK#',1);
#Categories
INSERT INTO Categories Values(1, 'Informations for all','','informations','1','N','Y',NOW(),NULL);
INSERT INTO Categories Values(2, 'Informations for sysadmins','','informations','1','N','Y',NOW(),NULL);
INSERT INTO Categories Values(5, 'Informations for administration','','informations','1','N','Y',NOW(),NULL);
INSERT INTO Categories Values(6, 'Unannounced Informations','','informations','1','N','Y',NOW(),NULL);
INSERT INTO GroupInCategories Values(1,1);
INSERT INTO GroupInCategories Values(4,1);
INSERT INTO GroupInCategories Values(1,2);
INSERT INTO GroupInCategories Values(4,5);

#Standard ACLs
INSERT INTO Enumerates VALUES(NULL,'apiAcl','myself.manage',6);
INSERT INTO Acls VALUES(NULL,NULL,1,'myself.modify','Y',6);
INSERT INTO Acls VALUES(NULL,NULL,4,'myself.modify','Y',6);
INSERT INTO Acls VALUES(NULL,NULL,4,'myself.search','Y',6);
#ACLS
