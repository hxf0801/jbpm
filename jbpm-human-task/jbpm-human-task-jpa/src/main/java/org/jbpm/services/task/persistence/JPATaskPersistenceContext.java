package org.jbpm.services.task.persistence;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.xml.datatype.XMLGregorianCalendar;

import org.drools.core.util.StringUtils;
import org.jbpm.persistence.processinstance.ProcessInstanceExtra;
import org.jbpm.services.task.impl.model.AttachmentImpl;
import org.jbpm.services.task.impl.model.CommentImpl;
import org.jbpm.services.task.impl.model.ContentImpl;
import org.jbpm.services.task.impl.model.DeadlineImpl;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.OrganizationalEntityImpl;
import org.jbpm.services.task.impl.model.TaskImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.task.api.model.Deadline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.services.task.persistence.TaskQueryManager.adaptQueryString;
import static org.kie.internal.query.QueryParameterIdentifiers.FILTER;
import static org.kie.internal.query.QueryParameterIdentifiers.FIRST_RESULT;
import static org.kie.internal.query.QueryParameterIdentifiers.FLUSH_MODE;
import static org.kie.internal.query.QueryParameterIdentifiers.MAX_RESULTS;
import static org.kie.internal.query.QueryParameterIdentifiers.ORDER_BY;
import static org.kie.internal.query.QueryParameterIdentifiers.ORDER_TYPE;

public class JPATaskPersistenceContext implements TaskPersistenceContext {

    // logger set to public for test reasons, see the org.jbpm.services.task.TaskQueryBuilderLocalTest
	public final static Logger logger = LoggerFactory.getLogger(JPATaskPersistenceContext.class);
	
	private static TaskQueryManager querymanager = TaskQueryManager.get();
	
	protected EntityManager em;
    protected final boolean isJTA;
    protected final boolean pessimisticLocking;
    
    public JPATaskPersistenceContext(EntityManager em) {
        this(em, true, false);
    }
    
    public JPATaskPersistenceContext(EntityManager em, boolean isJTA) {
       this(em, isJTA, false); 
    }
    
    public JPATaskPersistenceContext(EntityManager em, boolean isJTA, boolean locking) {
        this.em = em;
        this.isJTA = isJTA;
        this.pessimisticLocking = locking;
        
        logger.debug("TaskPersistenceManager configured with em {}, isJTA {}, pessimistic locking {}", new Object[] {em, isJTA, locking});
    }	
	
	@Override
	public Task findTask(Long taskId) {
		check();
		TaskImpl task = null;
		if (this.pessimisticLocking) {
			task = this.em.find(TaskImpl.class, taskId,
					LockModeType.PESSIMISTIC_FORCE_INCREMENT);
		} else {
			task = this.em.find(TaskImpl.class, taskId);
		}
		if (null != task) {
			TaskData taskData = task.getTaskData();
			if (null != taskData) {
				ProcessInstanceExtra processInstanceExtra =
						doInternalFindProcessInstanceExtra(taskData.getProcessInstanceId());
				if (null != processInstanceExtra) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("site_code", processInstanceExtra.getSiteCode());
					map.put("service_code", processInstanceExtra.getServiceCode());
					map.put("company_code", processInstanceExtra.getCompanyCode());
					map.put("process_group", processInstanceExtra.getProcessGroup());
					map.put("item_key", processInstanceExtra.getItemKey());
					map.put("item_type", processInstanceExtra.getItemType());
					map.put("opt_type", processInstanceExtra.getOptType());
					map.put("text1", processInstanceExtra.getText1());
					map.put("text2", processInstanceExtra.getText2());
					map.put("text3", processInstanceExtra.getText3());
					map.put("text4", processInstanceExtra.getText4());
					map.put("text5", processInstanceExtra.getText5());
					map.put("char1", processInstanceExtra.getChar1());
					map.put("char2", processInstanceExtra.getChar2());
					map.put("money1", processInstanceExtra.getMoney1());
					map.put("money2", processInstanceExtra.getMoney2());
					map.put("money3", processInstanceExtra.getMoney3());
					map.put("integer1", processInstanceExtra.getInteger1());
					map.put("integer2", processInstanceExtra.getInteger2());
					map.put("decimal1", processInstanceExtra.getDecimal1());
					map.put("decimal2", processInstanceExtra.getDecimal2());
					map.put("date1", processInstanceExtra.getDate1());
					map.put("date2", processInstanceExtra.getDate2());
					map.put("date3", processInstanceExtra.getDate3());
					map.put("timestamp1", processInstanceExtra.getTimestamp1());
					map.put("timestamp2", processInstanceExtra.getTimestamp2());
					map.put("wfe_client_id", processInstanceExtra.getWfeClientIdentifier());
					map.put("bu_name", processInstanceExtra.getBuName());
					task.setMoreProperties(map);
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("find the specified task with extra table >>> " + task.getMoreProperties());
			}
		}
		return task;
	}

	@Override
	public Task persistTask(Task task) {
		check();
		this.em.persist( task );
        if( this.pessimisticLocking ) { 
        	this.em.flush();
            return this.em.find(TaskImpl.class, task.getId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT );
        }
        return task;
	}

	@Override
	public Task updateTask(Task task) {
		check();
		return this.em.merge(task);
	}

	@Override
	public Task removeTask(Task task) {
		check();
		em.remove( task );
		
		return task;
	}

	@Override
	public Group findGroup(String groupId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( GroupImpl.class, groupId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( GroupImpl.class, groupId );
	}

	@Override
	public Group persistGroup(Group group) {
		check();
		try {
			this.em.persist( group );
	        if( this.pessimisticLocking ) { 
	        	this.em.flush();
	            return this.em.find(GroupImpl.class, group.getId(), LockModeType.PESSIMISTIC_WRITE );
	        }
		} catch (EntityExistsException e) {
    		throw new RuntimeException("Group already exists with " + group 
    				+ " id, please check that there is no group and user with same id");
    	}
        return group;
	}

	@Override
	public Group updateGroup(Group group) {
		check();
		return this.em.merge(group);
	}

	@Override
	public Group removeGroup(Group group) {
		check();
		em.remove( group );
		return group;
	}

	@Override
	public User findUser(String userId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( UserImpl.class, userId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( UserImpl.class, userId );
	}

	@Override
	public User persistUser(User user) {
		check();
		try {
			this.em.persist( user );
	        if( this.pessimisticLocking ) { 
	        	this.em.flush();
	            return this.em.find(UserImpl.class, user.getId(), LockModeType.PESSIMISTIC_WRITE );
	        }
		} catch (EntityExistsException e) {
    		throw new RuntimeException("User already exists with " + user 
    				+ " id, please check that there is no group and user with same id");
    	}
        return user;
	}

	@Override
	public User updateUser(User user) {
		check();
		return this.em.merge(user);
	}

	@Override
	public User removeUser(User user) {
		check();
		em.remove( user );
		return user;
	}

	@Override
	public OrganizationalEntity findOrgEntity(String orgEntityId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( OrganizationalEntityImpl.class, orgEntityId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( OrganizationalEntityImpl.class, orgEntityId );
	}

	@Override
	public OrganizationalEntity persistOrgEntity(OrganizationalEntity orgEntity) {
		check();
	    	
        if (!StringUtils.isEmpty(orgEntity.getId())) {
        	try {
	        	this.em.persist( orgEntity );
	            if( this.pessimisticLocking ) { 
	            	this.em.flush();
	                return this.em.find(OrganizationalEntityImpl.class, orgEntity.getId(), LockModeType.PESSIMISTIC_WRITE );
	            }
        	} catch (EntityExistsException e) {
        		throw new RuntimeException("Organizational entity already exists with " + orgEntity 
        				+ " id, please check that there is no group and user with same id");
        	}
        } 
		
        return orgEntity;
	}

	@Override
	public OrganizationalEntity updateOrgEntity(OrganizationalEntity orgEntity) {
		check();
		return this.em.merge(orgEntity);
	}

	@Override
	public OrganizationalEntity removeOrgEntity(OrganizationalEntity orgEntity) {
		check();
		em.remove( orgEntity );
		return orgEntity;
	}

	@Override
	public Content findContent(Long contentId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( ContentImpl.class, contentId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( ContentImpl.class, contentId );
	}

	@Override
	public Content persistContent(Content content) {
		check();
		this.em.persist( content );
        if( this.pessimisticLocking ) { 
        	this.em.flush();
            return this.em.find(ContentImpl.class, content.getId(), LockModeType.PESSIMISTIC_WRITE );
        }
        return content;
	}

	@Override
	public Content updateContent(Content content) {
		check();
		return this.em.merge(content);
	}

	@Override
	public Content removeContent(Content content) {
		check();
		em.remove( content );
		return content;
	}

	@Override
	public Attachment findAttachment(Long attachmentId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( AttachmentImpl.class, attachmentId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( AttachmentImpl.class, attachmentId );
	}

	@Override
	public Attachment persistAttachment(Attachment attachment) {
		check();
		this.em.persist( attachment );
        if( this.pessimisticLocking ) { 
        	this.em.flush();
            return this.em.find(AttachmentImpl.class, attachment.getId(), LockModeType.PESSIMISTIC_WRITE );
        }
        return attachment;
	}

	@Override
	public Attachment updateAttachment(Attachment attachment) {
		check();
		return this.em.merge(attachment);
	}

	@Override
	public Attachment removeAttachment(Attachment attachment) {
		check();
		em.remove( attachment );
		return attachment;
	}

	@Override
	public Comment findComment(Long commentId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( CommentImpl.class, commentId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( CommentImpl.class, commentId );
	}

	@Override
	public Comment persistComment(Comment comment) {
		check();
		this.em.persist( comment );
        if( this.pessimisticLocking ) { 
        	this.em.flush();
            return this.em.find(CommentImpl.class, comment.getId(), LockModeType.PESSIMISTIC_WRITE );
        }
        return comment;
	}

	@Override
	public Comment updateComment(Comment comment) {
		check();
		return this.em.merge(comment);
	}

	@Override
	public Comment removeComment(Comment comment) {
		check();
		em.remove( comment );
		return comment;
	}

	@Override
	public Deadline findDeadline(Long deadlineId) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( DeadlineImpl.class, deadlineId, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( DeadlineImpl.class, deadlineId );
	}

	@Override
	public Deadline persistDeadline(Deadline deadline) {
		check();
		this.em.persist( deadline );
        if( this.pessimisticLocking ) { 
        	this.em.flush();
            return this.em.find(DeadlineImpl.class, deadline.getId(), LockModeType.PESSIMISTIC_WRITE );
        }
        return deadline;
	}

	@Override
	public Deadline updateDeadline(Deadline deadline) {	
		check();
		return this.em.merge(deadline);
	}

	@Override
	public Deadline removeDeadline(Deadline deadline) {
		check();
		em.remove( deadline );
		return deadline;
	}

	@Override
	public <T> T queryWithParametersInTransaction(String queryName,
			Map<String, Object> params, Class<T> clazz) {
		check();
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, false, LockModeType.NONE, clazz, query);
	}

        @Override
	public <T> T queryWithParametersInTransaction(String queryName, boolean singleResult,
			Map<String, Object> params, Class<T> clazz) {
		check();
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, singleResult, LockModeType.NONE, clazz, query);
	}
        
	@Override
	public <T> T queryAndLockWithParametersInTransaction(String queryName,
			Map<String, Object> params, boolean singleResult, Class<T> clazz) {
		check();
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, singleResult, LockModeType.NONE, clazz, query);
	}

	@Override
	public <T> T queryInTransaction(String queryName, Class<T> clazz) {
		check();
		Query query = this.em.createNamedQuery(queryName);
		return (T) query.getResultList();
	}

	@Override
	public <T> T queryStringInTransaction(String queryString, Class<T> clazz) {
		check();
		Query query = this.em.createQuery(queryString);
		return (T) query.getResultList();
	}

	@Override
	public <T> T queryStringWithParametersInTransaction(String queryString,
			Map<String, Object> params, Class<T> clazz) {
		check();
		String newQueryString = adaptQueryString(new StringBuilder(queryString), params);
		if( newQueryString != null ) { 
		    queryString = newQueryString;
		}
		
		// logging
		logger.debug("QUERY:\n {}", queryString);
		if( logger.isDebugEnabled() ) {
		    StringBuilder paramsStr = new StringBuilder("PARAMS:");
		    Map<String, Object> orderedParams = new TreeMap<String, Object>(params);
		    for( Entry<String, Object> entry : orderedParams.entrySet() ) { 
		        paramsStr.append("\n " + entry.getKey() + " : '" + entry.getValue() + "'");
		    }
		    logger.debug(paramsStr.toString());
		}
		
		Query query = this.em.createQuery(queryString);
				
		return queryStringWithParameters(params, false, LockModeType.NONE, clazz, query);
	}
	
	@Override
	public <T> T queryStringWithParametersInTransaction(String queryString, boolean singleResult,
			Map<String, Object> params, Class<T> clazz) {
		check();
		Query query = this.em.createQuery(queryString);
				
		return queryStringWithParameters(params, singleResult, LockModeType.NONE, clazz, query);
	}

	
	@Override	
	public <T> T queryAndLockStringWithParametersInTransaction(
			String queryName, Map<String, Object> params, boolean singleResult,
			Class<T> clazz) {
		check();
		Query query = getQueryByName(queryName, params);
		return queryStringWithParameters(params, singleResult, LockModeType.PESSIMISTIC_WRITE, clazz, query);	
	}

	@Override
	public int executeUpdateString(String updateString) {
		check();
		Query query = this.em.createQuery(updateString);
		return query.executeUpdate();
	}

	@Override
	public HashMap<String, Object> addParametersToMap(Object... parameterValues) {
		HashMap<String, Object> parameters = new HashMap<String, Object>();
        
        if( parameterValues.length % 2 != 0 ) { 
            throw new RuntimeException("Expected an even number of parameters, not " + parameterValues.length);
        }
        
        for( int i = 0; i < parameterValues.length; ++i ) {
            String parameterName = null;
            if( parameterValues[i] instanceof String ) { 
                parameterName = (String) parameterValues[i];
            } else { 
                throw new RuntimeException("Expected a String as the parameter name, not a " + parameterValues[i].getClass().getSimpleName());
            }
            ++i;
            parameters.put(parameterName, parameterValues[i]);
        }
        
        return parameters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T persist(T object) {
		check();
		this.em.persist( object ); 
		if( this.pessimisticLocking ) { 
			this.em.flush();
			Object primaryKey = getFieldValueWithAnnotation(object, Id.class);			
            return (T) this.em.find( object.getClass(), primaryKey, LockModeType.PESSIMISTIC_WRITE );
        }
        return object;
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		check();
		if( this.pessimisticLocking ) { 
            return this.em.find( entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE );
        }
        return this.em.find( entityClass, primaryKey );
	}

	@Override
	public <T> T remove(T entity) {
		check();
		em.remove( entity );
		return entity;
	}

	@Override
	public <T> T merge(T entity) {
		check();
		return this.em.merge(entity);
	}

	private <T> T queryStringWithParameters(Map<String, Object> params, boolean singleResult, LockModeType lockMode,
			Class<T> clazz, Query query) {
		
		if (lockMode != null) {
			query.setLockMode(lockMode);
		}
		if (params != null && !params.isEmpty()) {
			for (Entry<String,Object> paramEntry : params.entrySet()) {
			    String name = paramEntry.getKey();
				if (FIRST_RESULT.equals(name)) {
					query.setFirstResult((Integer) paramEntry.getValue());
					continue;
				} else if (MAX_RESULTS.equals(name)) {
					if (((Integer) paramEntry.getValue()) > -1) {
						query.setMaxResults((Integer) paramEntry.getValue());
					}
					continue;
				} else if (FLUSH_MODE.equals(name)) {
					query.setFlushMode(FlushModeType.valueOf((String) paramEntry.getValue()));
					continue;
				} 
				// skip control parameters
				else if ( ORDER_TYPE.equals(name)
				        || ORDER_BY.equals(name)
						|| FILTER.equals(name)) {
					continue;
				}
				query.setParameter(name, params.get(name));
			}
		}
		if (singleResult) {
                    List<T> results = query.getResultList();
                    return (T) ((results.isEmpty() )? null : results.get(0));
		}
		return (T) query.getResultList();
	}

	@Override
	public boolean isOpen() {
		if (this.em == null) {
			return false;
		}
		return this.em.isOpen();
	}

	@Override
	public void joinTransaction() {
		if (this.em == null) {
			return;
		}
		if (this.isJTA) {
			this.em.joinTransaction();
		}
	}

	@Override
	public void close() {
		check();
		this.em.close();
	}
	
	protected void check() {
		if (em == null || !em.isOpen()) {
			throw new IllegalStateException("Entity manager is null or is closed, exiting...");
		}
	}
	
	protected Query getQueryByName(String queryName, Map<String, Object> params) {
		String queryStr = querymanager.getQuery(queryName, params);
		Query query = null;
		if (queryStr != null) {
			query = this.em.createQuery(queryStr);
		} else {
			query = this.em.createNamedQuery(queryName);
		}
		
		return query;
	}
	
	private Object getFieldValueWithAnnotation(Object object, Class<? extends Annotation> annotation) {
		try {
			Field[] fields = object.getClass().getDeclaredFields();
			
			for (Field f : fields) {
				if (f.isAnnotationPresent(annotation)) {
					f.setAccessible(true);
					return f.get(object);
				}
			}
		} catch (Exception e) {
			logger.error("Unable to find primary key of class {} sure to {}", object.getClass(), e.getMessage());
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> nativequeryWithParametersInTransaction(String queryString, Map<String, Object> params) {
		check();
		Query query = this.em.createNativeQuery(queryString);

		if (params != null && !params.isEmpty()) {
			for (String name : params.keySet()) {
				if (FIRST_RESULT.equals(name)) {
					query.setFirstResult((Integer) params.get(name));
					continue;
				}
				if (MAX_RESULTS.equals(name)) {
					query.setMaxResults((Integer) params.get(name));
					continue;
				}
				if (FLUSH_MODE.equals(name)) {
					query.setFlushMode(FlushModeType.valueOf((String) params.get(name)));
					continue;
				}
				
				int position = 0;
				try {
					position = Integer.parseInt(name);
				} catch (Exception e) {
					logger.warn("Name[" + name + "] is not a valid number, using named parameter");
					position = 0;
				}
				if (position >= 1) {
				    Object o = params.get(name);
				    logger.trace("*******parameter class=" + o.getClass().getName() + ", value=" + o);
                    if (o instanceof XMLGregorianCalendar) {
                        query.setParameter(position, ((XMLGregorianCalendar) o).toGregorianCalendar().getTime());
                    } else {
                        query.setParameter(position, o);
                    }
				} else {
					query.setParameter(name, params.get(name));
				}
			}
		}
		return query.getResultList();
	}

	@Override
	public int nativequeryTotalRows(String queryString, Map<String, Object> params) {
		check();
		Query query = this.em.createNativeQuery(queryString);

		if (params != null && !params.isEmpty()) {
			for (String name : params.keySet()) {
				if (FLUSH_MODE.equals(name)) {
					query.setFlushMode(FlushModeType.valueOf((String) params.get(name)));
					continue;
				}
				if (MAX_RESULTS.equals(name) || FIRST_RESULT.equals(name)) {
					continue;
				}
				
				int position = 0;
				try {
					position = Integer.parseInt(name);
				} catch (Exception e) {
					logger.warn("Name[" + name + "] is not a valid number, using named parameter");
					position = 0;
				}
				if (position >= 1) {
				    Object o = params.get(name);
				    logger.trace("*******parameter class=" + o.getClass().getName() + ", value=" + o);
                    if (o instanceof XMLGregorianCalendar) {
                        query.setParameter(position, ((XMLGregorianCalendar) o).toGregorianCalendar().getTime());
                    } else {
                        query.setParameter(position, o);
                    }
				} else {
					query.setParameter(name, params.get(name));
				}
			}
		}
		Object obj = query.getSingleResult();
		if (null != obj) {
			logger.trace("***** returned result class type = " + obj.getClass().getName());
		}
		return ((BigDecimal)obj).intValue();
	}

	@Override
	public void updateProcessExtra(Long taskId, Map<String, Object> data) {
		check();
		TaskImpl task = null;
		if (this.pessimisticLocking) {
			task = this.em.find(TaskImpl.class, taskId,
					LockModeType.PESSIMISTIC_FORCE_INCREMENT);
		} else {
			task = this.em.find(TaskImpl.class, taskId);
		}
		if (null != task) {
			TaskData taskData = task.getTaskData();
			if (null != taskData) {
				ProcessInstanceExtra processInstanceExtra =
					doInternalFindProcessInstanceExtra(taskData.getProcessInstanceId());
				if (null != processInstanceExtra) {
					if (logger.isDebugEnabled()) {
						logger.debug("updating extra table >>> " + processInstanceExtra);
					}
					processInstanceExtra.updateState(data);
					this.em.merge(processInstanceExtra);
					if (logger.isDebugEnabled()) {
						logger.debug("updated extra table >>> " + processInstanceExtra);
					}
				}
			}
		}
	}

    @Override
    public void updateProcessExtraByInstanceId(Long processInstanceId, Map<String, Object> data) {
        check();
        ProcessInstanceExtra processInstanceExtra = doInternalFindProcessInstanceExtra(processInstanceId);
        if (null != processInstanceExtra) {
            if (logger.isDebugEnabled()) {
                logger.debug("updating extra table >>> " + processInstanceExtra);
            }
            processInstanceExtra.updateState(data);
            this.em.merge(processInstanceExtra);
            if (logger.isDebugEnabled()) {
                logger.debug("updated extra table >>> " + processInstanceExtra);
            }
        }
    }

    /**
     * Find extra table object in a method
     * 
     * @param processInstanceId
     *        -long, process instance id
     * @return ProcessInstanceExtra
     * @author PTI
     */
    private ProcessInstanceExtra doInternalFindProcessInstanceExtra(long processInstanceId) {
        if (this.pessimisticLocking) {
            return this.em
                          .find(ProcessInstanceExtra.class, processInstanceId, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return this.em.find(ProcessInstanceExtra.class, processInstanceId);
    }
}
