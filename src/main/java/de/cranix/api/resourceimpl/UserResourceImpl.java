/* (c) 2022 Péter Varkoly <peter@varkoly.de> - all rights reserved */
package de.cranix.api.resourceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.cranix.api.resources.UserResource;
import de.cranix.dao.*;
import de.cranix.services.Service;
import de.cranix.services.GroupService;
import de.cranix.services.UserService;
import de.cranix.helper.CommonEntityManagerFactory;
import de.cranix.helper.OSSShellTools;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static de.cranix.helper.CranixConstants.cranixTmpDir;

public class UserResourceImpl implements UserResource {

    Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    public UserResourceImpl() {
    }

    @Override
    public User getById(Session session, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final User user = userService.getById(userId);
        em.close();
        if (user == null) {
            throw new WebApplicationException(404);
        }
        return user;
    }

    @Override
    public List<User> getByRole(Session session, String role) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final List<User> users = userService.getByRole(role);
        em.close();
        if (users == null) {
            throw new WebApplicationException(404);
        }
        return users;
    }

    @Override
    public List<User> getAll(Session session) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final List<User> users = userService.getAll();
        em.close();
        if (users == null) {
            throw new WebApplicationException(404);
        }
        return users;
    }

    @Override
    public CrxResponse insert(Session session, User user) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new UserService(session, em).add(user);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse add(Session session, User user) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new UserService(session, em).add(user);
        em.close();
        if (crxResponse.getCode().equals("OK")) {
            sync(session);
        }
        return crxResponse;
    }

    @Override
    public List<CrxResponse> add(Session session, List<User> users) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        List<CrxResponse> crxResponses = new UserService(session, em).add(users);
        sync(session);
        em.close();
        return crxResponses;
    }

    @Override
    public CrxResponse delete(Session session, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new UserService(session, em).delete(userId);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse modify(Session session, User user) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        CrxResponse crxResponse = userService.modify(user);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse modify(Session session, Long userId, User user) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        user.setId(userId);
        final UserService userService = new UserService(session, em);
        CrxResponse crxResponse = userService.modify(user);
        em.close();
        return crxResponse;
    }

    @Override
    public List<User> search(Session session, String search) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final List<User> users = userService.search(search);
        em.close();
        if (users == null) {
            throw new WebApplicationException(404);
        }
        return users;
    }

    @Override
    public List<Group> getAvailableGroups(Session session, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final List<Group> groups = userService.getAvailableGroups(userId);
        em.close();
        if (groups == null) {
            throw new WebApplicationException(404);
        }
        return groups;
    }

    @Override
    public List<Group> groups(Session session, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final List<Group> groups = userService.getGroups(userId);
        em.close();
        if (groups == null) {
            throw new WebApplicationException(404);
        }
        return groups;
    }

    @Override
    public CrxResponse setMembers(Session session, Long userId, List<Long> groupIds) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new UserService(session, em).setGroups(userId, groupIds);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse removeMember(Session session, Long groupId, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final GroupService groupService = new GroupService(session, em);
        CrxResponse crxResponse = groupService.removeMember(groupId, userId);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse addToGroups(Session session, Long userId, List<Long> groups) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        StringBuilder error = new StringBuilder();
        final GroupService groupService = new GroupService(session, em);
        for (Long groupId : groups) {
            CrxResponse crxResponse = groupService.addMember(groupId, userId);
            if (!crxResponse.getCode().equals("OK")) {
                error.append(crxResponse.getValue()).append("<br>");
            }
        }
        em.close();
        if (error.length() > 0) {
            return new CrxResponse(session, "ERROR", error.toString());
        }
        return new CrxResponse(session, "OK", "User was added to the additional group.");
    }

    @Override
    public CrxResponse addMember(Session session, Long groupId, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final GroupService groupService = new GroupService(session, em);
        CrxResponse crxResponse = groupService.addMember(groupId, userId);
        em.close();
        return crxResponse;
    }

    @Override
    public CrxResponse syncFsQuotas(Session session, List<List<String>> Quotas) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        CrxResponse crxResponse = userService.syncFsQuotas(Quotas);
        em.close();
        return crxResponse;
    }

    @Override
    public List<User> getUsers(Session session, List<Long> userIds) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        List<User> users = userService.getUsers(userIds);
        em.close();
        return users;
    }

    @Override
    public String getUserAttribute(Session session, String uid, String attribute) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        String resp;
        User user = userService.getByUid(uid);
        if (user == null) {
            return "";
        }
        switch (attribute.toLowerCase()) {
            case "id":
                resp = String.valueOf(user.getId());
                break;
            case "role":
                resp = user.getRole();
                break;
            case "uuid":
                resp = user.getUuid();
                break;
            case "givenname":
                resp = user.getGivenName();
                break;
            case "surname":
                resp = user.getSurName();
                break;
            case "home":
                resp = userService.getHomeDir(user);
                break;
            case "groups":
                List<String> groups = new ArrayList<String>();
                for (Group group : user.getGroups()) {
                    groups.add(group.getName());
                }
                resp = String.join(userService.getNl(), groups);
                break;
            default:
                //This is a config or mconfig. We have to merge it from the groups from actual room and from the user
                List<String> configs = new ArrayList<String>();
                //Group configs
                for (Group group : user.getGroups()) {
                    if (userService.getConfig(group, attribute) != null) {
                        configs.add(userService.getConfig(group, attribute));
                    }
                    for (String config : userService.getMConfigs(group, attribute)) {
                        if (config != null) {
                            configs.add(config);
                        }
                    }
                }
                //Room configs.
                if (session.getRoom() != null) {
                    if (userService.getConfig(session.getRoom(), attribute) != null) {
                        configs.add(userService.getConfig(session.getRoom(), attribute));
                    }
                    for (String config : userService.getMConfigs(session.getRoom(), attribute)) {
                        if (config != null) {
                            configs.add(config);
                        }
                    }
                }
                if (userService.getConfig(user, attribute) != null) {
                    configs.add(userService.getConfig(user, attribute));
                }
                for (String config : userService.getMConfigs(user, attribute)) {
                    if (config != null) {
                        configs.add(config);
                    }
                }
                resp = String.join(userService.getNl(), configs);
        }
        em.close();
        return resp;
    }

    @Override
    public List<Category> getGuestUsers(Session session) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        List<Category> resp = new UserService(session, em).getGuestUsers();
        em.close();
        return resp;
    }

    @Override
    public Category getGuestUsersCategory(Session session, Long guestUsersId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        Category resp = new UserService(session, em).getGuestUsersCategory(guestUsersId);
        em.close();
        return resp;
    }

    @Override
    public CrxResponse deleteGuestUsers(Session session, Long guestUsersId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse resp = new UserService(session, em).deleteGuestUsers(guestUsersId);
        em.close();
        return resp;
    }

    @Override
    public CrxResponse addGuestUsers(Session session, String name, String description, Long roomId, Long count,
                                     Date validUntil) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        GuestUsers guestUsers = new GuestUsers(name, description, count, roomId, validUntil);
        CrxResponse resp = new UserService(session, em).addGuestUsers(guestUsers);
        em.close();
        return resp;
    }

    @Override
    public String getGroups(Session session, String userName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        String resp = new UserService(session, em).getGroupsOfUser(userName, "workgroup");
        em.close();
        return resp;
    }

    @Override
    public String getClasses(Session session, String userName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        String resp = new UserService(session, em).getGroupsOfUser(userName, "class");
        em.close();
        return resp;
    }

    @Override
    public String addToGroup(Session session, String userName, String groupName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new GroupService(session, em).addMember(groupName, userName);
        String resp = crxResponse.getCode();
        if (crxResponse.getCode().equals("ERROR")) {
            resp = resp + " " + crxResponse.getValue();
        }
        em.close();
        return resp;
    }


    @Override
    public String addGroupToUser(Session session, String userName, String groupName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new GroupService(session, em).setOwner(groupName, userName);
        String resp = crxResponse.getCode();
        if (crxResponse.getCode().equals("ERROR")) {
            resp = resp + " " + crxResponse.getValue();
        }
        em.close();
        return resp;
    }

    @Override
    public String addUserAlias(Session session, String userName, String alias) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        UserService uc = new UserService(session, em);
        String resp = "Alias not unique";
        if (uc.isUserAliasUnique(alias)) {
            User user = uc.getByUid(userName);
            if (user != null) {
                Alias newAlias = new Alias(user, alias);
                em.getTransaction().begin();
                em.persist(newAlias);
                user.getAliases().add(newAlias);
                em.merge(user);
                em.getTransaction().commit();
                resp = "Alias was created";
            } else {
                resp = "User can not be found";
            }
        }
        em.close();
        return resp;
    }

    @Override
    public String addUserDefaultAlias(Session session, String userName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        UserService uc = new UserService(session, em);
        User user = uc.getByUid(userName);
        CrxResponse crxResponse = uc.addDefaultAliase(user);
        String resp = crxResponse.getCode();
        if (crxResponse.getCode().equals("ERROR")) {
            resp = resp + " " + crxResponse.getValue();
        }
        em.close();
        return resp;
    }

    @Override
    public String removeFromGroup(Session session, String userName, String groupName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new GroupService(session, em).removeMember(groupName, userName);
        String resp = crxResponse.getCode();
        if (crxResponse.getCode().equals("ERROR")) {
            resp = resp + " " + crxResponse.getValue();
        }
        em.close();
        return resp;
    }

    @Override
    public String delete(Session session, String userName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse crxResponse = new UserService(session, em).delete(userName);
        String resp = crxResponse.getCode();
        if (crxResponse.getCode().equals("ERROR")) {
            resp = resp + " " + crxResponse.getValue();
        }
        em.close();
        return resp;
    }

    @Override
    public String createUid(Session session, String givenName, String surName, String birthDay) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        String resp = new UserService(session, em).createUid(givenName, surName, birthDay);
        em.close();
        return resp;
    }

    @Override
    public CrxResponse importUser(
            Session session,
            String role,
            String lang,
            String identifier,
            Boolean test,
            String password,
            Boolean mustChange,
            Boolean full,
            Boolean allClasses,
            Boolean cleanClassDirs,
            Boolean resetPassword,
            Boolean appendBirthdayToPassword,
            Boolean appendClassToPassword,
            InputStream fileInputStream,
            FormDataContentDisposition contentDispositionHeader) {
        File file = null;
        try {
            file = File.createTempFile("crx", "importUser", new File(cranixTmpDir));
            Files.copy(fileInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new CrxResponse(session, "ERROR", "Import file can not be saved" + e.getMessage());
        }
        try {
            Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            logger.debug("Import file is not UTF-8 try ISO-8859-1");
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
                List<String> utf8lines = new ArrayList<String>();
                for (String line : lines) {
                    byte[] utf8 = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1).getBytes(StandardCharsets.UTF_8);
                    utf8lines.add(new String(utf8, StandardCharsets.UTF_8));
                }
                Files.write(file.toPath(), utf8lines);

            } catch (IOException ioe2) {
                return new CrxResponse(session, "ERROR", "Import file is not UTF-8 coded.");
            }
        }
        if (password != null && !password.isEmpty()) {
            UserService uc = new UserService(session, null);
            Boolean checkPassword = uc.getConfigValue("CHECK_PASSWORD_QUALITY").toLowerCase().equals("yes");
            uc.setConfigValue("CHECK_PASSWORD_QUALITY", "no");
            CrxResponse passwordResponse = uc.checkPassword(password);
            if (passwordResponse != null) {
                if (checkPassword) {
                    uc.setConfigValue("CHECK_PASSWORD_QUALITY", "yes");
                }
                logger.error("Reset Password" + passwordResponse);
                return passwordResponse;
            }
        }
        List<String> parameters = new ArrayList<String>();
        parameters.add("/sbin/startproc");
        parameters.add("-l");
        parameters.add("/var/log/import-user.log");
        parameters.add("/usr/sbin/crx_import_user_list.py");
        parameters.add("--input");
        parameters.add(file.getAbsolutePath());
        parameters.add("--role");
        parameters.add(role);
        parameters.add("--lang");
        parameters.add(lang);
        if (identifier != null && !identifier.isEmpty()) {
            parameters.add("--identifier");
            parameters.add(identifier);
        }
        if (test) {
            parameters.add("--test");
        }
        if (password != null && !password.isEmpty()) {
            parameters.add("--password");
            parameters.add(password);
        }
        if (mustChange) {
            parameters.add("--mustChange");
        }
        if (full) {
            parameters.add("--full");
        }
        if (allClasses) {
            parameters.add("--allClasses");
        }
        if (cleanClassDirs) {
            parameters.add("--cleanClassDirs");
        }
        if (resetPassword) {
            parameters.add("--resetPassword");
        }
        if (appendBirthdayToPassword) {
            parameters.add("--appendBirthdayToPassword");
        }
        if (appendClassToPassword) {
            parameters.add("--appendClassToPassword");
        }
        if (logger.isDebugEnabled()) {
            parameters.add("--debug");
        }
        String[] program = new String[parameters.size()];
        program = parameters.toArray(program);

        logger.debug("Start import:" + parameters);
        StringBuffer reply = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        OSSShellTools.exec(program, reply, stderr, null);
        return new CrxResponse(session, "OK", "Import was started.");
    }

    @Override
    public List<UserImport> getImports(Session session) {
        Service controller = new Service(session, null);
        StringBuilder importDir = controller.getImportDir("");
        List<UserImport> imports = new ArrayList<UserImport>();
        File importDirObject = new File(importDir.toString());
        if (importDirObject.isDirectory()) {
            for (String file : importDirObject.list()) {
                UserImport userImport = getImport(session, file.replaceAll(importDir.append("/").toString(), ""));
                if (userImport != null) {
                    imports.add(userImport);
                }
            }
        }
        return imports;
    }

    @Override
    public UserImport getImport(Session session, String startTime) {
        Service controller = new Service(session, null);
        String content;
        UserImport userImport;
        String importLog = controller.getImportDir(startTime).append("/import.log").toString();
        String importJson = controller.getImportDir(startTime).append("/parameters.json").toString();
        ObjectMapper mapper = new ObjectMapper();
        logger.debug("getImport 1:" + startTime);
        try {
            content = String.join("", Files.readAllLines(Paths.get(importJson)));
            userImport = mapper.readValue(IOUtils.toInputStream(content, "UTF-8"), UserImport.class);
        } catch (IOException e) {
            logger.debug("getImport 2:" + e.getMessage());
            return null;
        }
        try {
            content = String.join("", Files.readAllLines(Paths.get(importLog), StandardCharsets.UTF_8));
            userImport.setResult(content);
        } catch (IOException e) {
            logger.debug("getImport 3:" + importLog + " " + content + "####" + e.getMessage());
        }
        return userImport;
    }

    @Override
    public CrxResponse restartImport(Session session, String startTime) {
        UserImport userImport = getImport(session, startTime);
        if (userImport != null) {
            Service controller = new Service(session, null);
            StringBuilder importFile = controller.getImportDir(startTime);
            importFile.append("/userlist.txt");
            List<String> parameters = new ArrayList<String>();
            parameters.add("/sbin/startproc");
            parameters.add("-l");
            parameters.add("/var/log/import-user.log");
            parameters.add("/usr/sbin/crx_import_user_list.py");
            parameters.add("--input");
            parameters.add(importFile.toString());
            parameters.add("--role");
            parameters.add(userImport.getRole());
            parameters.add("--lang");
            parameters.add(userImport.getLang());
            parameters.add("--identifier");
            parameters.add(userImport.getIdentifier());
            if (!userImport.getPassword().isEmpty()) {
                parameters.add("--password");
                parameters.add(userImport.getPassword());
            }
            if (userImport.isMustChange()) {
                parameters.add("--mustChange");
            }
            if (userImport.isFull()) {
                parameters.add("--full");
            }
            if (userImport.isAllClasses()) {
                parameters.add("--allClasses");
            }
            if (userImport.isCleanClassDirs()) {
                parameters.add("--cleanClassDirs");
            }
            if (userImport.isResetPassword()) {
                parameters.add("--resetPassword");
            }
            if (logger.isDebugEnabled()) {
                parameters.add("--debug");
            }
            logger.debug("restartImport userImport:" + userImport);
            logger.debug("restartImport parameters:" + parameters);

            String[] program = new String[parameters.size()];
            program = parameters.toArray(program);

            logger.debug("Start import:" + parameters);
            StringBuffer reply = new StringBuffer();
            StringBuffer stderr = new StringBuffer();
            OSSShellTools.exec(program, reply, stderr, null);
            logger.debug("restartImport reply: " + reply.toString());
            logger.debug("restartImport error: " + reply.toString());
            return new CrxResponse(session, "OK", "Import was started.");
        }
        return new CrxResponse(session, "ERROR", "CAn not find the import.");
    }

    @Override
    public CrxResponse deleteImport(Session session, String startTime) {
        Service controller = new Service(session, null);
        StringBuilder importDir = controller.getImportDir(startTime);
        if (startTime == null || startTime.isEmpty()) {
            return new CrxResponse(session, "ERROR", "Invalid import name.");
        }
        String[] program = new String[3];
        program[0] = "rm";
        program[1] = "-rf";
        program[2] = importDir.toString();
        StringBuffer reply = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        OSSShellTools.exec(program, reply, stderr, null);
        return new CrxResponse(session, "OK", "Import was deleted.");
    }

    @Override
    public CrxResponse stopRunningImport(Session session) {
        String[] program = new String[2];
        program[0] = "killall";
        program[1] = "crx_import_user_list.py";
        StringBuffer reply = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        OSSShellTools.exec(program, reply, stderr, null);
        program = new String[3];
        program[0] = "rm";
        program[1] = "-f";
        program[2] = "/run/crx_import_user";
        OSSShellTools.exec(program, reply, stderr, null);
        return new CrxResponse(session, "OK", "Import was stopped.");
    }

    @Override
    public UserImport getRunningImport(Session session) {
        List<String> runningImport;
        try {
            runningImport = Files.readAllLines(Paths.get("/run/crx_import_user"));
            if (!runningImport.isEmpty()) {
                return getImport(session, runningImport.get(0));
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    @Override
    public Response getImportAsPdf(Session session, String startTime) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response getImportAsTxt(Session session, String startTime) {
        Service controller = new Service(session, null);
        StringBuilder importDir = controller.getImportDir(startTime);
        String[] program = new String[2];
        program[0] = "/usr/share/cranix/tools/pack_import.sh";
        program[1] = startTime;
        StringBuffer reply = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        OSSShellTools.exec(program, reply, stderr, null);
        File importFile = new File(importDir.append("/userimport.zip").toString());
        ResponseBuilder response = Response.ok(importFile);
        response = response.header("Content-Disposition", "attachment; filename=" + importFile.getName());
        return response.build();

    }

    @Override
    public CrxResponse syncMsQuotas(Session session, List<List<String>> Quotas) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        CrxResponse resp = new UserService(session, em).syncMsQuotas(Quotas);
        em.close();
        return resp;
    }

    @Override
    public String getUidsByRole(Session session, String role) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        List<String> users = new ArrayList<String>();
        for (User user : userService.getByRole(role)) {
            users.add(user.getUid());
        }
        String resp = String.join(userService.getNl(), users);
        em.close();
        return resp;
    }

    @Override
    public CrxResponse allTeachersInAllClasses(Session session) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final UserService userService = new UserService(session, em);
        final GroupService groupService = new GroupService(session, em);
        for (User user : userService.getByRole("teachers")) {
            for (Group group : groupService.getByType("class")) {
                groupService.addMember(group, user);
            }
        }
        em.close();
        return new CrxResponse(session, "OK", "All teachers was put into all classes.");
    }

    @Override
    public CrxResponse allClasses(Session session, Long userId) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        User user = new UserService(session, em).getById(userId);
        final GroupService groupService = new GroupService(session, em);
        for (Group group : groupService.getByType("class")) {
            groupService.addMember(group, user);
        }
        em.close();
        return new CrxResponse(session, "OK", "User was put into all classes.");
    }

    @Override
    public String addToAllClasses(Session session, String userName) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        User user = new UserService(session, em).getByUid(userName);
        final GroupService groupService = new GroupService(session, em);
        for (Group group : groupService.getByType("class")) {
            groupService.addMember(group, user);
        }
        em.close();
        return "OK";
    }

    @Override
    public CrxResponse sync(Session session) {
        //TODO make it over plugin
        String[] program = new String[1];
        program[0] = "/usr/sbin/crx_refresh_squidGuard_user.sh";
        StringBuffer reply = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        OSSShellTools.exec(program, reply, stderr, null);
        return new CrxResponse(session, "OK", "Import was started.");
    }

    @Override
    public CrxResponse addUsersToGroups(Session session, List<List<Long>> ids) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        final GroupService groupService = new GroupService(session, em);
        List<User> users = new UserService(session, em).getUsers(ids.get(0));
        for (Long groupId : ids.get(1)) {
            Group group = groupService.getById(groupId);
            groupService.addMembers(group, users);
        }
        em.close();
        return new CrxResponse(session, "OK", "Users was inserted in the required groups.");
    }

    @Override
    public List<CrxResponse> applyAction(Session session, CrxActionMap crxActionMap) {
        EntityManager em = CommonEntityManagerFactory.instance("dummy").getEntityManagerFactory().createEntityManager();
        List<CrxResponse> responses = new ArrayList<CrxResponse>();
        UserService userService = new UserService(session, em);
        logger.debug(crxActionMap.toString());
        switch (crxActionMap.getName().toLowerCase()) {
            case "setpassword":
                return userService.resetUserPassword(
                        crxActionMap.getObjectIds(),
                        crxActionMap.getStringValue(),
                        crxActionMap.isBooleanValue());
            case "setfilesystemquota":
                return userService.setFsQuota(
                        crxActionMap.getObjectIds(),
                        crxActionMap.getLongValue());
            case "setmailsystemquota":
                return userService.setMsQuota(
                        crxActionMap.getObjectIds(),
                        crxActionMap.getLongValue());
            case "disablelogin":
                return userService.disableLogin(
                        crxActionMap.getObjectIds(),
                        true);
            case "enablelogin":
                return userService.disableLogin(
                        crxActionMap.getObjectIds(),
                        false);
            case "disableinternet":
                return userService.disableInternet(
                        crxActionMap.getObjectIds(),
                        true);
            case "enableinternet":
                return userService.disableInternet(
                        crxActionMap.getObjectIds(),
                        false);
            case "mandatoryprofile":
                return userService.mandatoryProfile(
                        crxActionMap.getObjectIds(),
                        true);
            case "openprofile":
                return userService.mandatoryProfile(
                        crxActionMap.getObjectIds(),
                        false);
            case "copytemplate":
                return userService.copyTemplate(
                        crxActionMap.getObjectIds(),
                        crxActionMap.getStringValue());
            case "removeprofiles":
                return userService.removeProfile(crxActionMap.getObjectIds());
            case "delete":
                for (Long userId : crxActionMap.getObjectIds()) {
                    User user = userService.getById(userId);
                    if (user != null) {
                        logger.debug("delete user:" + user);
                        responses.add(userService.delete(user));
                    }
                }
        }
        em.close();
        return responses;
    }

}
