package de.openschoolserver.api.resourceimpl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import de.openschoolserver.api.resources.SchedulerResource;
import de.openschoolserver.dao.Category;
import de.openschoolserver.dao.OssResponse;
import de.openschoolserver.dao.Session;
import de.openschoolserver.dao.controller.UserController;

public class SchedulerResourceImpl implements SchedulerResource {

	public SchedulerResourceImpl() {
		
	}
	@Override
	public OssResponse deleteExpieredGuestUser(Session session) {
		UserController uc = new UserController(session);
		EntityManager em = uc.getEntityManager();
		Query query = em.createNamedQuery("Category.expiredByType").setParameter("type", "guestUser");
		Integer counter = 0;
		for(Category category : (List<Category>) query.getResultList() ) {
			uc.deleteGuestUsers(category.getId());
			counter++;
		}
		if( counter == 0 ) {
			return new OssResponse(session,"OK","No guest user accounts to delete.");
		} 
		return new OssResponse(session,"OK","%s guest user groups was deleted.",null,counter.toString());
	}

}