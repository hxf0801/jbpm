package org.jbpm.persistence;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.drools.persistence.SingleSessionCommandService;
import org.drools.persistence.TransactionManager;
import org.drools.persistence.TransactionManagerHelper;
import org.drools.persistence.jpa.JpaPersistenceContext;
import org.jbpm.persistence.correlation.CorrelationKeyInfo;
import org.jbpm.persistence.processinstance.JPASignalManager;
import org.jbpm.persistence.processinstance.ProcessInstanceExtra;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.ProcessInstanceManager;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationProperty;

public class JpaProcessPersistenceContext extends JpaPersistenceContext
    implements
    ProcessPersistenceContext {
    
    public JpaProcessPersistenceContext(EntityManager em, TransactionManager txm) {
        super( em, txm );
    }

    public JpaProcessPersistenceContext(EntityManager em, boolean useJTA, boolean locking, TransactionManager txm) {
        super( em, useJTA, locking, txm);
    }

    public ProcessInstanceInfo persist(ProcessInstanceInfo processInstanceInfo) {
        EntityManager em = getEntityManager();
        em.persist(processInstanceInfo);
        TransactionManagerHelper.addToUpdatableSet(txm, processInstanceInfo);
        if( this.pessimisticLocking ) { 
        	em.flush();
            return em.find(ProcessInstanceInfo.class, processInstanceInfo.getId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT );
        }
        return processInstanceInfo;
    }

    public ProcessInstanceInfo findProcessInstanceInfo(Long processId) {
    	EntityManager em = getEntityManager();       
    	if( this.pessimisticLocking ) { 
            return em.find( ProcessInstanceInfo.class, processId, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
        }
        return em.find( ProcessInstanceInfo.class, processId );
    }

    public void remove(ProcessInstanceInfo processInstanceInfo) {
        getEntityManager().remove( processInstanceInfo );
        TransactionManagerHelper.removeFromUpdatableSet(txm, processInstanceInfo);
        List<CorrelationKeyInfo> correlations = getEntityManager().createNamedQuery("GetCorrelationKeysByProcessInstanceId")
        .setParameter("pId", processInstanceInfo.getId()).getResultList();
        if (correlations != null) {
            for (CorrelationKeyInfo key : correlations) {
                getEntityManager().remove(key);
            }
        }
    }

    /**
     * This method is used by the {@link JPASignalManager} in order to load {@link ProcessInstance} instances
     * into the {@link ProcessInstanceManager} cache so that they can then be signalled. 
     * </p>
     * Unfortunately, with regards to locking, the method is not always called during a transaction, which means 
     * that including logic to lock the query will cause exceptions and is not feasible. 
     * </p>
     * Because the {@link SingleSessionCommandService} design is based around a synchronized execute(...) method, 
     * it's not possible for one thread to create a process instance while another thread simultaneously tries to 
     * signal it. That means that a 
     * <a href="http://en.wikipedia.org/wiki/Isolation_%28database_systems%29#Phantom_reads">phantom read</a> 
     * race condition, that might be caused by a lack of pessimistic locking on this query, isn't possible. 
     * </p>
     * Of course, if you're using multiple ksessions to simultaneoulsy interact with the same process instance, 
     * all bets are off. This however is true for almost everything involving process instances, so that it's not 
     * worth discussing. 
     * </p>
     */
    public List<Long> getProcessInstancesWaitingForEvent(String type) {
    	EntityManager entityManager = getEntityManager();
    	if (entityManager != null) {
	        Query processInstancesForEvent = getEntityManager().createNamedQuery( "ProcessInstancesWaitingForEvent" );
	        processInstancesForEvent.setParameter( "type",
	                                               type );
	        return (List<Long>) processInstancesForEvent.getResultList();
    	} else {
    		// entity manager can be null when fireActivationCreated is
    		// called on session unmarshalling
    		return new ArrayList<Long>();
    	}
    }

    public CorrelationKeyInfo persist(CorrelationKeyInfo correlationKeyInfo) {
        Long processInstanceId = getProcessInstanceByCorrelationKey(correlationKeyInfo);
        if (processInstanceId != null) {
            throw new RuntimeException(correlationKeyInfo + " already exists");
        }
        EntityManager em = getEntityManager();
        em.persist( correlationKeyInfo );
        if( this.pessimisticLocking) {
        	em.flush();
            return em.find(CorrelationKeyInfo.class, correlationKeyInfo.getId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return correlationKeyInfo;
        
    }

    /**
     * With regards to locking, the method is not always called during a transaction, which means 
     * that including logic to lock the query will cause exceptions and is not feasible. 
     * </p>
     * However, this is not an issue: see the {@link #getProcessInstancesWaitingForEvent(String)} documentation
     * for more information. The same logic applies to this method. 
     * </p>
     */
    public Long getProcessInstanceByCorrelationKey(CorrelationKey correlationKey) {
        Query processInstancesForEvent = getEntityManager().createNamedQuery( "GetProcessInstanceIdByCorrelation" );
        processInstancesForEvent.setParameter( "elem_count", correlationKey.getProperties().size() );
        List<Object> properties = new ArrayList<Object>();
        for (CorrelationProperty<?> property : correlationKey.getProperties()) {
            properties.add(property.getValue());
        }
        processInstancesForEvent.setParameter( "properties", properties );
        try {
            return (Long) processInstancesForEvent.getSingleResult();
        } catch (NonUniqueResultException e) {
            return null;
        } catch (NoResultException e) {
            return null;
        }
    }

	@Override
	public ProcessInstanceExtra persist(ProcessInstanceExtra processInstanceExtra) {
        EntityManager em = getEntityManager();
        em.persist(processInstanceExtra);
        if( this.pessimisticLocking ) { 
            return em.find(ProcessInstanceExtra.class, processInstanceExtra.getProcessInstanceId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT );
        }
        return processInstanceExtra;
	}

	private ProcessInstanceExtra findProcessInstanceExtra(Long processInstanceId) {
		if( this.pessimisticLocking ) { 
            return getEntityManager().find( ProcessInstanceExtra.class, processInstanceId, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
        }
        return getEntityManager().find( ProcessInstanceExtra.class, processInstanceId );
	}

	private void remove(ProcessInstanceExtra processInstanceExtra) {
        if(null != processInstanceExtra){
        	getEntityManager().remove( processInstanceExtra );
        }
	}
    
}
