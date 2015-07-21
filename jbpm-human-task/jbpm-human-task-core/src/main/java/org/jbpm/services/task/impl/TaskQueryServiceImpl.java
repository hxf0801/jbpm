/**
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jbpm.services.task.impl;

import static org.kie.internal.query.QueryParameterIdentifiers.ACTUAL_OWNER_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.ASCENDING_VALUE;
import static org.kie.internal.query.QueryParameterIdentifiers.BUSINESS_ADMIN_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.CREATED_BY_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.DEPLOYMENT_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.DESCENDING_VALUE;
import static org.kie.internal.query.QueryParameterIdentifiers.FILTER;
import static org.kie.internal.query.QueryParameterIdentifiers.FIRST_RESULT;
import static org.kie.internal.query.QueryParameterIdentifiers.MAX_RESULTS;
import static org.kie.internal.query.QueryParameterIdentifiers.ORDER_BY;
import static org.kie.internal.query.QueryParameterIdentifiers.ORDER_TYPE;
import static org.kie.internal.query.QueryParameterIdentifiers.POTENTIAL_OWNER_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.PROCESS_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.PROCESS_INSTANCE_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.STAKEHOLDER_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.TASK_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.TASK_STATUS_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.WORK_ITEM_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.FLUSH_MODE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.FlushModeType;

import org.jbpm.services.task.impl.model.UserImpl;
import org.jbpm.services.task.query.TaskSummaryImpl;
import org.jbpm.services.task.utils.ClassUtil;
import org.kie.api.search.SearchCriteria;
import org.kie.api.search.WhereParameter;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryAndParameterAppender;
import org.kie.internal.query.QueryContext;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.query.QueryModificationService;
import org.kie.internal.query.data.QueryData;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.task.api.TaskQueryService;
import org.kie.internal.task.api.model.InternalTaskSummary;
import org.kie.internal.task.api.model.SubTasksStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pti.fsc.common.wf.WfTaskSummary;
import com.pti.fsc.wfe.util.WfeUserBuCallback;

/**
 *
 */
public class TaskQueryServiceImpl implements TaskQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TaskQueryServiceImpl.class);
    
    private TaskPersistenceContext persistenceContext;
    private UserGroupCallback userGroupCallback;
    
    protected List<?> adoptList(List<?> source, List<?> values) {
    	
    	if (source == null || source.isEmpty()) {
    		List<Object> data = new ArrayList<Object>();    		
    		for (Object value : values) {
    			data.add(value);
    		}
    		
    		return data;
    	}
    	return source;
    }
    
    protected void applyQueryFilter(Map<String, Object> params, QueryFilter queryFilter) {
    	if (queryFilter != null) {
    	    applyQueryContext(params, queryFilter);
        	if (queryFilter.getFilterParams() != null && !queryFilter.getFilterParams().isEmpty()) {
        		params.put(FILTER, queryFilter.getFilterParams());
        		for(String key : queryFilter.getParams().keySet()){
                    params.put(key, queryFilter.getParams().get(key));
                }
        	}
        }
    }
    
    protected void applyQueryContext(Map<String, Object> params, QueryContext queryContext) {
    	if (queryContext != null) {
    	    Integer offset = queryContext.getOffset(); 
    	    if( offset != null && offset > 0 ) { 
    	        params.put(FIRST_RESULT, offset);
    	    }
    	    Integer count = queryContext.getCount();
    	    if( count != null && count > 0 ) { 
    	        params.put(MAX_RESULTS, count);
    	    }
        	
        	if (queryContext.getOrderBy() != null && !queryContext.getOrderBy().isEmpty()) {
        		params.put(ORDER_BY, queryContext.getOrderBy());
        
        		if( queryContext.isAscending() != null ) { 
        		    if (queryContext.isAscending()) {
        		        params.put(ORDER_TYPE, ASCENDING_VALUE);
        		    } else {
        		        params.put(ORDER_TYPE, DESCENDING_VALUE);
        		    }
        		}
        	}
    	}
    }
    
    private static final List<Status> allActiveStatus = new ArrayList<Status>(){{
        this.add(Status.Created);
        this.add(Status.Ready);
        this.add(Status.Reserved);
        this.add(Status.InProgress);
        this.add(Status.Suspended);
      }};

    public TaskQueryServiceImpl() {
    }
    
    public TaskQueryServiceImpl(TaskPersistenceContext persistenceContext, UserGroupCallback userGroupCallback) {
    	this.persistenceContext = persistenceContext;
    	this.userGroupCallback = userGroupCallback;
    }

    public void setPersistenceContext(TaskPersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    public void setUserGroupCallback(UserGroupCallback userGroupCallback) {
        this.userGroupCallback = userGroupCallback;
    }
    
    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsBusinessAdministrator",
        		persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedAsExcludedOwner(String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsExcludedOwner", 
        		persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwner", 
        		persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
                
    }

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds) {
        if(groupIds == null || groupIds.isEmpty()){
          return getTasksAssignedAsPotentialOwner(userId);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        params.put("groupIds", groupIds);
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerWithGroups", 
                params,
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedByGroup(String groupId) {
        if(groupId == null || groupId.isEmpty()){
          return Collections.EMPTY_LIST;
        }
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerByGroup", 
                persistenceContext.addParametersToMap("groupId", groupId ),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedByGroupsByExpirationDateOptional(List<String> groupIds, Date expirationDate) {
        if(groupIds == null || groupIds.isEmpty()){
          return Collections.EMPTY_LIST;
        }
        List<Object[]> tasksByGroups = (List<Object[]>)persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerByGroupsByExpirationDateOptional", 
                persistenceContext.addParametersToMap("groupIds", groupIds, "expirationDate", expirationDate),
                ClassUtil.<List<Object[]>>castClass(List.class));
                
        return collectTasksByPotentialOwners(tasksByGroups);
    }  
    
    protected List<TaskSummary> collectTasksByPotentialOwners(List<Object[]> tasksByGroups) {
        Set<Long> tasksIds = Collections.synchronizedSet(new HashSet<Long>());
        Map<Long, List<String>> potentialOwners = Collections.synchronizedMap(new HashMap<Long, List<String>>());
        for (Object o : tasksByGroups) {
            Object[] get = (Object[]) o;
            tasksIds.add((Long) get[0]);
            if (potentialOwners.get((Long) get[0]) == null) {
                potentialOwners.put((Long) get[0], new ArrayList<String>());
            }
            potentialOwners.get((Long) get[0]).add((String) get[1]);
        }
        if (!tasksIds.isEmpty()) {
            List<TaskSummary> tasks = (List<TaskSummary>)persistenceContext.queryWithParametersInTransaction("TaskSummariesByIds", 
                    persistenceContext.addParametersToMap("taskIds", tasksIds),
                    ClassUtil.<List<TaskSummary>>castClass(List.class));
                    

            for (TaskSummary ts : tasks) {
                ((InternalTaskSummary) ts).setPotentialOwners(potentialOwners.get(ts.getId()));
            }
            return tasks;
        }
        return new ArrayList<TaskSummary>();
    }
    
    public List<TaskSummary> getTasksAssignedByGroupsByExpirationDate(List<String> groupIds, Date expirationDate) {
        if(groupIds == null || groupIds.isEmpty()){
          return Collections.EMPTY_LIST;
        }
        List<Object[]> tasksByGroups = (List<Object[]>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerByGroupsByExpirationDate", 
                persistenceContext.addParametersToMap("groupIds", groupIds, "expirationDate", expirationDate),
                ClassUtil.<List<Object[]>>castClass(List.class));
        return collectTasksByPotentialOwners(tasksByGroups);
    }        
            
    public List<TaskSummary> getTasksAssignedByGroups(List<String> groupIds) {
        if(groupIds == null || groupIds.isEmpty()){
          return Collections.EMPTY_LIST;
        }
        List<Object[]> tasksByGroups = (List<Object[]>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerByGroups", 
                persistenceContext.addParametersToMap("groupIds", groupIds),
                ClassUtil.<List<Object[]>>castClass(List.class));
                
        Set<Long> tasksIds = Collections.synchronizedSet(new HashSet<Long>());
        Map<Long, List<String>> potentialOwners = Collections.synchronizedMap(new HashMap<Long, List<String>>());
        for (Object o : tasksByGroups) {
            Object[] get = (Object[]) o;
            tasksIds.add((Long) get[0]);
            if (potentialOwners.get((Long) get[0]) == null) {
                potentialOwners.put((Long) get[0], new ArrayList<String>());
            }
            potentialOwners.get((Long) get[0]).add((String) get[1]);
        }
        if (!tasksIds.isEmpty()) {
            List<TaskSummary> tasks = (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TaskSummariesByIds", 
                        persistenceContext.addParametersToMap("taskIds", tasksIds),
                        ClassUtil.<List<TaskSummary>>castClass(List.class));

            for (TaskSummary ts : tasks) {
                ((InternalTaskSummary) ts).setPotentialOwners(potentialOwners.get(ts.getId()));
            }
            return tasks;
        }
        return new ArrayList<TaskSummary>();
    }

    public Map<Long, List<OrganizationalEntity>> getPotentialOwnersForTaskIds(List<Long> taskIds){
        List<Object[]> potentialOwners = persistenceContext.queryWithParametersInTransaction("GetPotentialOwnersForTaskIds", 
                persistenceContext.addParametersToMap("taskIds", taskIds),
                ClassUtil.<List<Object[]>>castClass(List.class));
        
        Map<Long, List<OrganizationalEntity>> potentialOwnersMap = new HashMap<Long, List<OrganizationalEntity>>();
        Long currentTaskId = 0L;
        for(Object[] item : potentialOwners){
            Long taskId = (Long) item[0];
            OrganizationalEntity potentialOwner = (OrganizationalEntity)item[1];
            if(currentTaskId != taskId){
                currentTaskId = taskId;
            }
            
            if(potentialOwnersMap.get(currentTaskId) == null){
                potentialOwnersMap.put(currentTaskId, new ArrayList<OrganizationalEntity>());
            }
            potentialOwnersMap.get(currentTaskId).add(potentialOwner);
        }
        
        return potentialOwnersMap;
    
    }
    
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, int firstResult, int maxResults) {
        if(groupIds == null || groupIds.isEmpty()){
          return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwner", 
                                    persistenceContext.addParametersToMap("userId", userId, 
                                                    "firstResult", firstResult, "maxResults", maxResults),
                                                    ClassUtil.<List<TaskSummary>>castClass(List.class));
        }
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerWithGroups", 
                                    persistenceContext.addParametersToMap("userId", userId, "groupIds", groupIds, 
                                                    "firstResult", firstResult, "maxResults", maxResults),
                                                    ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedAsRecipient(String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsRecipient", 
                persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedAsTaskInitiator(String userId) {
        return (List<TaskSummary>)  persistenceContext.queryWithParametersInTransaction("TasksAssignedAsTaskInitiator", 
                persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksAssignedAsTaskStakeholder(String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsTaskStakeholder", 
                persistenceContext.addParametersToMap("userId", userId),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    public List<TaskSummary> getTasksOwned(String userId) {
        return getTasksOwned(userId, null, null);

    }
    
   
    

    public List<TaskSummary> getTasksOwnedByStatus(String userId, List<Status> status) {

        List<TaskSummary> taskOwned =  getTasksOwned(userId, null, null);

        if (!taskOwned.isEmpty()) {
            Set<Long> tasksIds = new HashSet<Long>();
            for (TaskSummary ts : taskOwned) {
                tasksIds.add(ts.getId());
            }

            List<Object[]> tasksPotentialOwners = (List<Object[]>) persistenceContext.queryWithParametersInTransaction("TasksOwnedPotentialOwnersByTaskIds",
                        persistenceContext.addParametersToMap("taskIds", tasksIds),
                        ClassUtil.<List<Object[]>>castClass(List.class));

            Map<Long, List<String>> potentialOwners = new HashMap<Long, List<String>>();
            for (Object o : tasksPotentialOwners) {
                Object[] get = (Object[]) o;
                tasksIds.add((Long) get[0]);
                if (potentialOwners.get((Long) get[0]) == null) {
                    potentialOwners.put((Long) get[0], new ArrayList<String>());
                }
                potentialOwners.get((Long) get[0]).add((String) get[1]);
            }
            for (TaskSummary ts : taskOwned) {
                ((InternalTaskSummary) ts).setPotentialOwners(potentialOwners.get(ts.getId()));
            }
        } else {
            return new ArrayList<TaskSummary>(0);
        }

        return taskOwned;
    }

    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status) {
        return getTasksAssignedAsPotentialOwner(userId, null, status, null);
                
    }

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, List<Status> status, QueryFilter filter) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        params.put("status", adoptList(status, allActiveStatus));        
        params.put("groupIds", adoptList(groupIds, Collections.singletonList("")));
        
        applyQueryFilter(params, filter);

        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("NewTasksAssignedAsPotentialOwner", 
                                        params,
                                        ClassUtil.<List<TaskSummary>>castClass(List.class));
                
    }


    public List<TaskSummary> getTasksOwned(String userId, List<Status> status, QueryFilter filter) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        if(status == null){
            status = new ArrayList<Status>();
            status.add(Status.Reserved);
            status.add(Status.InProgress);
        }
        params.put("status", status);
        applyQueryFilter(params, filter);

        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("NewTasksOwned", 
                                        params,
                                        ClassUtil.<List<TaskSummary>>castClass(List.class));
    }
    
    
    public List<TaskSummary> getSubTasksAssignedAsPotentialOwner(long parentId, String userId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("SubTasksAssignedAsPotentialOwner",
                                        persistenceContext.addParametersToMap("parentId", parentId, "userId", userId),
                                        ClassUtil.<List<TaskSummary>>castClass(List.class));
                
    }

    public List<TaskSummary> getSubTasksByParent(long parentId) {
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("GetSubTasksByParentTaskId", 
                persistenceContext.addParametersToMap("parentId", parentId),
                ClassUtil.<List<TaskSummary>>castClass(List.class)); 
                
    }

    public int getPendingSubTasksByParent(long parentId) {
        return  persistenceContext.queryWithParametersInTransaction("GetSubTasksByParentTaskId", 
                                persistenceContext.addParametersToMap("parentId", parentId),
                                ClassUtil.<List<TaskSummary>>castClass(List.class)).size();
    }

    public Task getTaskInstanceById(long taskId) {
        Task taskInstance = persistenceContext.findTask(taskId);
        return taskInstance;

    }

    public Task getTaskByWorkItemId(long workItemId) {
        List<Task> tasks = (List<Task>)persistenceContext.queryWithParametersInTransaction("TaskByWorkItemId", 
                                persistenceContext.addParametersToMap("workItemId", workItemId,"maxResults", 1),
                                ClassUtil.<List<Task>>castClass(List.class));
        if (tasks.isEmpty())
            return null;
        else 
            return (Task) (tasks.get(0));
    }
    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDate(String userId, List<String> groupIds,
                                            List<Status> status, Date expirationDate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("expirationDate", expirationDate);
        
        return (List<TaskSummary>) getTasksAssignedAsPotentialOwner(userId, groupIds, status,
                new QueryFilter("t.taskData.expirationTime = :expirationDate", params, "order by t.id", false));
        
        

    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDateOptional(String userId, List<String> groupIds,
                        List<Status> status, Date expirationDate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("expirationDate", expirationDate);
        return (List<TaskSummary>) getTasksAssignedAsPotentialOwner(userId, groupIds, status,
                new QueryFilter("(t.taskData.expirationTime = :expirationDate or t.taskData.expirationTime is null)", params, "order by t.id", false));
        
    }
    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDate(String userId,  List<Status> status, Date expirationDate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("expirationDate", expirationDate);
        return (List<TaskSummary>) getTasksOwned(userId, status,
                new QueryFilter( "t.taskData.expirationTime = :expirationDate", params, "order by t.id", false));
        
        

    }
   

    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDateOptional(String userId, List<Status> status, Date expirationDate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("expirationDate", expirationDate);
        return (List<TaskSummary>) getTasksOwned(userId, status,
                new QueryFilter( "(t.taskData.expirationTime = :expirationDate or t.taskData.expirationTime is null)"
                        , params, "order by t.id", false));
        
    }
    
    @Override
    public List<TaskSummary> getTasksOwnedByExpirationDateBeforeSpecifiedDate(String userId, List<Status> status, Date date) {
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksOwnedWithParticularStatusByExpirationDateBeforeSpecifiedDate",
                persistenceContext.addParametersToMap("userId", userId, "status", status, "date", date),
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceId(long processInstanceId, List<Status> status) {
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }
        List<TaskSummary> tasks = (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksByStatusByProcessId",
                persistenceContext.addParametersToMap("processInstanceId", processInstanceId, 
                                        "status", status),
                                        ClassUtil.<List<TaskSummary>>castClass(List.class));
    
        return tasks;
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceIdByTaskName(long processInstanceId, List<Status> status, String taskName) {
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }    
        List<TaskSummary> tasks = (List<TaskSummary>)persistenceContext.queryWithParametersInTransaction("TasksByStatusByProcessIdByTaskName", 
                persistenceContext.addParametersToMap("processInstanceId", processInstanceId,
                                        "status", status, 
                                        "taskName", taskName),
                                        ClassUtil.<List<TaskSummary>>castClass(List.class));
    
        return tasks;
    }

    @Override
    public List<Long> getTasksByProcessInstanceId(long processInstanceId) {
        List<Long> tasks = (List<Long>)persistenceContext.queryWithParametersInTransaction("TasksByProcessInstanceId",
                persistenceContext.addParametersToMap("processInstanceId", processInstanceId),
                ClassUtil.<List<Long>>castClass(List.class));
        return tasks;
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDate(String userId, List<Status> status, Date expirationDate) {
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerStatusByExpirationDate",
                          persistenceContext.addParametersToMap("userId", userId, "groupIds", "", "status", status, "expirationDate", expirationDate),
                          ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByExpirationDateOptional(String userId, List<Status> status, Date expirationDate) {
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("TasksAssignedAsPotentialOwnerStatusByExpirationDateOptional",
                    persistenceContext.addParametersToMap("userId", userId, "groupIds", "", "status", status, "expirationDate", expirationDate),
                    ClassUtil.<List<TaskSummary>>castClass(List.class)); 
    }
   
    // This method should be deleted in jBPM 7.x+
    @Deprecated
    public List<TaskSummary> getTasksByVariousFields(String userId, List<Long> workItemIds, List<Long> taskIds, List<Long> procInstIds,
            List<String> busAdmins, List<String> potOwners, List<String> taskOwners, 
            List<Status> status,  boolean union, Integer maxResults) {
        Map<String, List<?>> params = new HashMap<String, List<?>>();
        params.put(WORK_ITEM_ID_LIST, workItemIds);
        params.put(TASK_ID_LIST, taskIds);
        params.put(PROCESS_INSTANCE_ID_LIST, procInstIds);
        params.put(BUSINESS_ADMIN_ID_LIST, busAdmins);
        params.put(POTENTIAL_OWNER_ID_LIST, potOwners);
        params.put(ACTUAL_OWNER_ID_LIST, taskOwners);
        if(status == null || status.isEmpty()){
          status = allActiveStatus;
        }
        params.put(TASK_STATUS_LIST, status);
        
        if( maxResults != null ) {
            if( maxResults <= 0 ) { 
                return new ArrayList<TaskSummary>();
            }
            Integer [] maxResultsArr = { maxResults };
            params.put(MAX_RESULTS, Arrays.asList(maxResultsArr));
        }
        
        return getTasksByVariousFields(userId, params, union);
    }
    
    // This method should be deleted in jBPM 7.x+
    @Deprecated
    public List<TaskSummary> getTasksByVariousFields( String userId, Map<String, List<?>> parameters, boolean union ) { 
        QueryData queryData = new QueryData();
        QueryContext queryContext = queryData.getQueryContext();
        if( queryContext.getOrderBy() == null || queryContext.getOrderBy().isEmpty() ) { 
            queryContext.setOrderBy("Id");
        }
        if( queryContext.isAscending() == null ) { 
            queryContext.setAscending(true);
        }
        List<?> maxResultsList = parameters.remove(MAX_RESULTS);
        if( maxResultsList != null && ! maxResultsList.isEmpty() ) { 
            Object maxResults = maxResultsList.get(0);
            if( maxResults instanceof Integer ) {
                queryContext.setCount((Integer) maxResults);
            }
        } 
        
        // convert parameters to query data
        if( union ) { 
            queryData.setToUnion();
        } else { 
            queryData.setToIntersection(); 
        }
        for( Entry<String, List<?>> paramEntry: parameters.entrySet() ) { 
            List<?> paramList = paramEntry.getValue();
            if( paramList != null && ! paramList.isEmpty() ) { 
                queryData.addAppropriateParam(paramEntry.getKey(), convertToTypedArray(paramList, paramList.get(0)));
            }
        }
        return query(userId, queryData);
    }
  
    private <T> T [] convertToTypedArray(List<?> paramList, T... firstElem) { 
        return paramList.toArray(firstElem);
    }
    
    public int getCompletedTaskByUserId(String userId) {
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Completed);
        List<TaskSummary> tasksCompleted = getTasksAssignedAsPotentialOwnerByStatus(userId, statuses);
        return tasksCompleted.size();
    }

    public int getPendingTaskByUserId(String userId) {
        List<TaskSummary> tasksAssigned = getTasksAssignedAsPotentialOwner(userId, null, null, null);
        return tasksAssigned.size();
    }
    
    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, 
                                                                        List<Status> status) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        params.put("status", adoptList(status, allActiveStatus));        
        params.put("groupIds", adoptList(groupIds, Collections.singletonList("")));
        
        return (List<TaskSummary>) persistenceContext.queryWithParametersInTransaction("QuickTasksAssignedAsPotentialOwnerWithGroupsByStatus", 
        		params,
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    /**
     * The following fields and methods provide a lookup table for the information needed to create a query based on
     * parameters in the {@link QueryData}. 
     */
    
    public static Map<String, Class<?>> criteriaFieldClasses = new ConcurrentHashMap<String, Class<?>>();
    public static Map<String, String> criteriaFields = new ConcurrentHashMap<String, String>();
    public static Map<String, String> criteriaFieldJoinClauses = new ConcurrentHashMap<String, String>();
   
    static { 
        addCriteria(PROCESS_ID_LIST, "t.taskData.processId", String.class);
        addCriteria(PROCESS_INSTANCE_ID_LIST, "t.taskData.processInstanceId", Long.class);
        addCriteria(WORK_ITEM_ID_LIST, "t.taskData.workItemId", Long.class);
        
        addCriteria(TASK_ID_LIST, "t.id", Long.class);
        addCriteria(DEPLOYMENT_ID_LIST, "t.taskData.deploymentId.id", String.class);
        addCriteria(TASK_STATUS_LIST, "t.taskData.status", Status.class);
        
        addCriteria(CREATED_BY_LIST, "t.taskData.createdBy.id", String.class);
        addCriteria(STAKEHOLDER_ID_LIST, "stakeHolders.id", String.class, 
                "stakeHolders in elements ( t.peopleAssignments.taskStakeholders )");
        addCriteria(POTENTIAL_OWNER_ID_LIST, "potentialOwners.id", String.class, 
                "potentialOwners in elements ( t.peopleAssignments.potentialOwners )");
        addCriteria(ACTUAL_OWNER_ID_LIST, "t.taskData.actualOwner.id", String.class);
        addCriteria(BUSINESS_ADMIN_ID_LIST, "businessAdministrators.id", String.class, 
                "businessAdministrators in elements ( t.peopleAssignments.businessAdministrators )");
    }
   
    private static void addCriteria( String listId, String fieldName, Class type ) { 
        addCriteria(listId, fieldName, type, null);
    }
    
    private static void addCriteria( String listId, String fieldName, Class type, String joinClause ) { 
        if( criteriaFields.put(listId, fieldName) != null ) { 
            throw new IllegalStateException("Duplicate field added for " + listId );
        }
        if( criteriaFieldClasses.put(listId, type ) != null ) { 
            throw new IllegalStateException("Duplicate field class added for " + listId );
        }
        if( joinClause != null ) { 
            if( criteriaFieldJoinClauses.put(listId, joinClause ) != null ) { 
                throw new IllegalStateException("Duplicate field join clause added for " + listId );
            }
        }
    }
   
    public static String TASKSUMMARY_SELECT =
                "SELECT distinct new org.jbpm.services.task.query.TaskSummaryImpl(\n" +
                "       t.id,\n" +
                "       t.name,\n" +
                "       t.description,\n" +
                "       t.taskData.status,\n" +
                "       t.priority,\n" +
                "       t.taskData.actualOwner.id,\n" +
                "       t.taskData.createdBy.id,\n" +
                "       t.taskData.createdOn,\n" +
                "       t.taskData.activationTime,\n" +
                "       t.taskData.expirationTime,\n" +
                "       t.taskData.processId,\n" +
                "       t.taskData.processInstanceId,\n" +
                "       t.taskData.parentId,\n" +
                "       t.taskData.deploymentId )\n";

    public static String TASKSUMMARY_FROM = 
                "FROM TaskImpl t,\n"
              + "     OrganizationalEntityImpl stakeHolders,\n"
              + "     OrganizationalEntityImpl potentialOwners,\n"
              + "     OrganizationalEntityImpl businessAdministrators\n";
    
    public static String TASKSUMMARY_WHERE = 
                "WHERE t.archived = 0\n";

    @Override
    public List<TaskSummary> query( String userId, QueryData queryData ) {
        // 1a. setup query
        StringBuilder queryBuilder = new StringBuilder(TASKSUMMARY_SELECT).append(TASKSUMMARY_FROM);
        
        // 1b. add other tables (used in kie-remote-services to cross-query on variables, etc.. )
        ServiceLoader<QueryModificationService> queryModServiceLdr = ServiceLoader.load(QueryModificationService.class);
        for( QueryModificationService queryModService : queryModServiceLdr ) { 
           queryModService.addTablesToQuery(queryBuilder, queryData);
        }
       
        // 1c. finish setup
        queryBuilder.append(TASKSUMMARY_WHERE);
        
        Map<String, Object> params = new HashMap<String, Object>();
        QueryAndParameterAppender queryAppender = new QueryAndParameterAppender(queryBuilder, params);
       
        // 2. check to see if we can results if possible by manipulating existing parameters
        GroupIdsCache groupIds = new GroupIdsCache(userId);
        boolean existingParametersUsedToLimitToAllowedResults = useExistingUserGroupIdToLimitResults(userId, queryData, groupIds);
        
        // 3a. add extended criteria 
        for( QueryModificationService queryModService : queryModServiceLdr ) { 
           queryModService.addCriteriaToQuery(queryBuilder, queryData, queryAppender);
        }
        
        // 3a. apply normal query parameters
        if( ! queryData.unionParametersAreEmpty() ) { 
            for( Entry<String, List<? extends Object>> paramsEntry : queryData.getUnionParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                Class<?> criteriaFieldClass = criteriaFieldClasses.get(listId);
                assert criteriaFieldClass != null : listId + ": criteria field class not found";
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addQueryParameters( paramsEntry.getValue(), listId, criteriaFieldClass, jpqlField, joinClause, true);
            }
        }
        if( ! queryData.intersectParametersAreEmpty() ) { 
            for( Entry<String, List<? extends Object>> paramsEntry : queryData.getIntersectParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                Class<?> criteriaFieldClass = criteriaFieldClasses.get(listId);
                assert criteriaFieldClass != null : listId + ": criteria field class not found";
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addQueryParameters(paramsEntry.getValue(), listId, criteriaFieldClass, jpqlField, joinClause, false);
            }
        }
        // 3b. apply range query parameters
        if( ! queryData.unionRangeParametersAreEmpty() ) { 
            for( Entry<String, List<? extends Object>> paramsEntry : queryData.getUnionRangeParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                Class<?> criteriaFieldClass = criteriaFieldClasses.get(listId);
                assert criteriaFieldClass != null : listId + ": criteria field class not found";
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addRangeQueryParameters(paramsEntry.getValue(), listId, criteriaFieldClass, jpqlField, joinClause, true);
            }
        }
        if( ! queryData.intersectRangeParametersAreEmpty() ) { 
            for( Entry<String, List<? extends Object>> paramsEntry : queryData.getIntersectRangeParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                Class<?> criteriaFieldClass = criteriaFieldClasses.get(listId);
                assert criteriaFieldClass != null : listId + ": criteria field class not found";
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addRangeQueryParameters(paramsEntry.getValue(), listId, criteriaFieldClass, jpqlField, joinClause, false);
            }
        }
        // 3c. apply regex query parameters
        if( ! queryData.unionRegexParametersAreEmpty() ) { 
            for( Entry<String, List<String>> paramsEntry : queryData.getUnionRegexParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addRegexQueryParameters(paramsEntry.getValue(), listId, jpqlField, joinClause, true);
            }
        }
        if( ! queryData.intersectRegexParametersAreEmpty() ) { 
            for( Entry<String, List<String>> paramsEntry : queryData.getIntersectRegexParameters().entrySet() ) { 
                String listId = paramsEntry.getKey();
                String jpqlField = criteriaFields.get(listId);
                assert jpqlField != null : listId + ": criteria field not found";
                String joinClause = criteriaFieldJoinClauses.get(listId);
                queryAppender.addRegexQueryParameters(paramsEntry.getValue(), listId, jpqlField, joinClause, false);
            }
        }
       
        // 4. close query clause, if parameters have been applied
        if( queryAppender.hasBeenUsed() ) { 
            queryBuilder.append(")"); 
        }
     
        // 5. add "limit tasks to viewable tasks" query if step 2 didn't succeed
        if( ! existingParametersUsedToLimitToAllowedResults ) { 
            addPossibleUserRolesQueryClause( userId, queryBuilder, groupIds, params, queryAppender );
        }
      
        // 6. apply meta info: max results, offset, order by, etc 
        String query = queryBuilder.toString();
        applyQueryContext(params, queryData.getQueryContext());
       
        // 7. Run the query!
        return persistenceContext.queryStringWithParametersInTransaction(query, params,
                ClassUtil.<List<TaskSummary>>castClass(List.class));
    }

    // actual owner and created by (initiator) can only be users
    private static String [] userParameterIds = { 
        ACTUAL_OWNER_ID_LIST,
        CREATED_BY_LIST
    };
    // ... but stakeholder, potential and bus. admin can be users orgroups
    private static String [] groupParameterIds = { 
        STAKEHOLDER_ID_LIST,
        POTENTIAL_OWNER_ID_LIST,
        BUSINESS_ADMIN_ID_LIST
    };
   
    /**
     * This is a small utility class which helps out 
     * with the {@link #useExistingUserGroupIdAsParameter(String[], QueryData, String...)}
     * and {@link #addPossibleUserRolesQueryClause(String, StringBuilder, GroupIdsCache, Map, QueryAndParameterAppender)}
     * methods.
     * </p>
     * It does 2 things: 
     * <ol>
     *   <li>Retrieve the group information via the {@link UserGroupCallback} field in the {@link TaskQueryServiceImpl}.</li>
     *   <li>Cache the group id information, because it may be retrieved in the first method mentioned above, and then 
     *   needed again in the second needed (if the first method does not succeed).</li>
     * </ol>
     */
    private class GroupIdsCache { 
       
        private final String userId; 
        private boolean groupsRetrieved = false;
        private List<String> groupIds;
       
        private GroupIdsCache(String userId ) {
           this.userId = userId; 
        }
       
        /**
         * If not already done, retrieve the group information via the {@link UserGroupCallback} instance and store it.
         */
        private List<String> getGroupIds() { 
            if( ! groupsRetrieved ) { 
                this.groupsRetrieved = true;
                this.groupIds = userGroupCallback.getGroupsForUser(userId, null, null);
            }
            return groupIds;
        }
        
        public String [] toArray() { 
            getGroupIds();
            return this.groupIds.toArray(new String[this.groupIds.size()]);
        }
        
        public int size() { 
            getGroupIds();
            return this.groupIds.size();
        }
        
    }

    /**
     * This class deals with the following: 
     * <ol>
     *   <li>If we're using a <em>union</em> of parameters, then we have to add a clause at the end 
     *       of the query saying the the user must be a intiator, stake holder, potential owner, etc.. 
     *       of the tasks retrieved.</li>
     *   <li>However, if the same user id is used in an intersection parameter, and the intersection parameter
     *   is one of the roles that the user must have in order to view the task<ul>
     *     <li><b>then</b>, there's no reason to also add a clause at the end of the query saying "only get tasks
     *     where the user has a 'good' role" -- the intersection parameter has already checked that, so to speak</li>
     *   </li>
     * </ol>  
     * 
     * In situation 2, this function verifies that this is indeed the case, and returns true if that's so. 
     * Furthermore, it also verifies the same thing for any of the groups that the user belongs to. See {@link GroupIdsCache} 
     * for more info there. 
     * 
     * @param userId The id of the user requesting the information. 
     * @param queryData The query data
     * @param groupIds A {@link GroupIdsCache} instance, which provides the groupids for the user id
     * @return whether or not a limiting query clause should be added to the end of the query
     */
    private boolean useExistingUserGroupIdToLimitResults(String userId, QueryData queryData, GroupIdsCache groupIds) { 
        // for the GetTasksByVariousFields method, which is actually broken and needs to be gotten rid of ASAP
        if( userId == null ) { 
            return true;
        }
        
        // if there are now intersect parameters used, we can not fortune tell (with only union parameters)
        if( queryData.intersectParametersAreEmpty() ) { 
            return false;
        }
        
        // this boolean tracks whether we (can) use an existing parameter to limit the results 
        // to tasks that the user is allowed to see
        boolean usedExistingParameters = true;
        
        // check if userId is used as a parameter
        usedExistingParameters = useExistingUserGroupIdAsParameter(userParameterIds, queryData, userId);
        
        if( ! usedExistingParameters ) { 
            // add/check the userId first, since that will probably have the best chance 
            //  of matching an owner type (initiator, actual, potential.. )
            String [] userIdsArr = new String[groupIds.size()+1];
            userIdsArr[0] = userId;
            System.arraycopy(groupIds.toArray(), 0, userIdsArr, 1, groupIds.size());
            // check if groupIds are used as parameters
            usedExistingParameters = useExistingUserGroupIdAsParameter(groupParameterIds, queryData, userIdsArr );
        }
       
        return usedExistingParameters;
    }
   
    /**
     * Determine whether or not we need to add a limiting clause 
     * ({@see #addPossibleUserRolesQueryClause(String, StringBuilder, GroupIdsCache, Map, QueryAndParameterAppender)}
     * at the end of the query
     * 
     * @param userGroupParamListIds The parameter list ids that we are looking for
     * @param queryData The query data
     * @param userGroupIds user and group ids from the user who called the query operation
     * @return true if we don't need to add another clause, false if we do
     */
    private boolean useExistingUserGroupIdAsParameter(String [] userGroupParamListIds, QueryData queryData, String... userGroupIds) { 
        for( String listId : userGroupParamListIds ) { 
            List<String> intersectListUserIds = (List<String>) queryData.getIntersectParameters().get(listId);
            if( intersectListUserIds != null ) { 
                for( String groupId : userGroupIds ) { 
                    if( intersectListUserIds.contains(groupId)) {
                        // yes! one of groupIds == one of intersectListUserIds
                        return true;
                    }
                }
            }
        }
        return false;
    }
   
    /**
     * Add a query clause to the end of the query limiting the result to the tasks that the user is allowed to see
     * @param userId the user id
     * @param queryBuilder The {@link StringBuilder} instance with the query string
     * @param groupIdsCache A cache of the user's group ids
     * @param params The params that will be set in the query
     * @param queryAppender The {@link QueryAndParameterAppender} instance being used
     */
    private void addPossibleUserRolesQueryClause(String userId, StringBuilder queryBuilder, GroupIdsCache groupIdsCache, Map<String, Object> params, 
            QueryAndParameterAppender queryAppender) { 
       
        // start phrase
        StringBuilder rolesQueryPhraseBuilder = new StringBuilder( "( " );
       
        // add criteria for catching tasks that refer to the user
        String userIdParamName = queryAppender.generateParamName();
        params.put(userIdParamName, userId);
        String groupIdsParamName = queryAppender.generateParamName();
        List<String> userAndGroupIds = new ArrayList<String>(1+groupIdsCache.size());
        userAndGroupIds.add(userId);
        userAndGroupIds.addAll(groupIdsCache.getGroupIds());
        params.put(groupIdsParamName, userAndGroupIds);
        
        rolesQueryPhraseBuilder.append("( ")
            .append("t.taskData.createdBy.id = :").append(userIdParamName).append("\n OR ")
            .append("( stakeHolders.id in :").append(groupIdsParamName).append(" and\n")
            .append("  stakeHolders in elements ( t.peopleAssignments.taskStakeholders ) )").append("\n OR " )
            .append("( potentialOwners.id in :").append(groupIdsParamName).append(" and\n")
            .append("  potentialOwners in elements ( t.peopleAssignments.potentialOwners ) )").append("\n OR " )
            .append("t.taskData.actualOwner.id = :").append(userIdParamName).append("\n OR ")
            .append("( businessAdministrators.id in :").append(groupIdsParamName).append(" and\n")
            .append("  businessAdministrators in elements ( t.peopleAssignments.businessAdministrators ) )")
            .append(" )\n");
        
        rolesQueryPhraseBuilder.append(") ");
            
        queryBuilder.append("\nAND ").append(rolesQueryPhraseBuilder);
    }

    /**
     * @author PTI
     */
    @Override
	public List<TaskSummary> getTasks(SearchCriteria searchCriteria) {
		String GENERIC_TASKSUM_QUERY =
			"select distinct t.id, t.processInstanceId, t.name as displayName, t.formname, t.subject, t.description,"
				+ " t.status, t.priority, t.skipable, t.actualOwner_id,"
				+ " t.createdBy_id, t.createdOn, t.activationTime, t.expirationTime, t.processId, t.processSessionId,"
				+ " t.subTaskStrategy, t.parentId,t.batch_process_type, p.EXTERNALID, propTable.site_code, propTable.service_code, propTable.company_code,"
				+ " propTable.process_group, propTable.item_key, propTable.item_type, propTable.opt_type, propTable.text1, propTable.text2, propTable.text3, propTable.text4, propTable.text5,"
				+ " propTable.char1, propTable.char2, propTable.money1, propTable.money2, propTable.money3, propTable.integer1, propTable.integer2,"
				+ " propTable.decimal1, propTable.decimal2, propTable.date1, propTable.date2, propTable.date3, propTable.timestamp1, propTable.timestamp2, propTable.wfe_client_id,propTable.Bu_Name "
				+ "from Task t, "
				+ " OrganizationalEntity businessAdministrator,"
				+ " OrganizationalEntity potentialOwners,"
				+ " ProcessInstanceLog p,"
				+ " ProcInstanceProp propTable "
				+ "where t.archived=0 and t.status in ('Created' , 'Ready' , 'Reserved' , 'InProgress' , 'Suspended')"
				+ " and p.processInstanceId=propTable.process_instance_id and p.processInstanceId=t.processInstanceId and ";
		StringBuilder queryBuilder = new StringBuilder(GENERIC_TASKSUM_QUERY);

		// handle the bu name parameter
		handleBuNameParam(searchCriteria);

		Map<String, Object> queryParams = new HashMap<String, Object>();
		queryParams.put(FLUSH_MODE, FlushModeType.COMMIT.toString());
		queryParams.put(FIRST_RESULT, searchCriteria.getFirstResult());
		if (searchCriteria.getMaxResults() != -1) {
			queryParams.put(MAX_RESULTS, searchCriteria.getMaxResults());
		}

		if (null != searchCriteria.getWhereStr() && !searchCriteria.getWhereStr().isEmpty()) {
			searchCriteria.setWhereStr(searchCriteria.getWhereStr());
		}

		logger.info("searchCriteria >>>" + searchCriteria);

		int nextPosition = 0;
		if (null != searchCriteria.getWhereStr() && !searchCriteria.getWhereStr().isEmpty()) {
			queryBuilder.append(searchCriteria.getWhereStr());
			List<WhereParameter> fields = searchCriteria.getParams();
			nextPosition = fields.size();
			for (WhereParameter paramObj : fields) {
				List<?> values = paramObj.getValues();
				if (null != values) {
					if (values.size() > 1) {
						Set<Object> paramVals = new HashSet<Object>();
						for (Object val : values) {
							if (val != null) {
								paramVals.add(val);
							}
						}
						queryParams.put(String.valueOf(paramObj.getParameterPosition()), paramVals);
					} else if (values.size() == 1) {
						queryParams.put(String.valueOf(paramObj.getParameterPosition()), values.get(0));
					}
				}
			}
		}
		// add bu filter 
		if(null != searchCriteria.getBuNames() && searchCriteria.getBuNames().size() > 0) {
			if (nextPosition > 0) {
				queryBuilder.append(" and ");
			}
			queryBuilder.append(" (propTable.Bu_Name is null or propTable.Bu_Name in (?").append(++nextPosition).append("))");
			Set<Object> paramVals = new HashSet<Object>();
			for (String str : searchCriteria.getBuNames()) {
				if (null != str) {
					paramVals.add(str);
				}
			}
			queryParams.put(String.valueOf(nextPosition), paramVals);
		}
		// add userId or groupIds
		if ((null != searchCriteria.getUserId() && !searchCriteria.getUserId().isEmpty())
			|| (null != searchCriteria.getGroupIds() && searchCriteria.getGroupIds().size() > 0)) {
			// (potentialOwners.id=?1 or potentialOwners.id in (?2)) and
			// (potentialOwners.id in (select pot_.entity_id from
			// PeopleAssignments_PotOwners pot_ where t.id=pot_.task_id))
			if (nextPosition > 0) {
				queryBuilder.append(" and ");
			}
			queryBuilder.append("(");
			boolean hasUserId = false;
			if (null != searchCriteria.getUserId() && !searchCriteria.getUserId().isEmpty()) {
				queryBuilder.append("potentialOwners.id=?").append(++nextPosition);
				queryParams.put(String.valueOf(nextPosition), searchCriteria.getUserId());
				hasUserId = true;
			}
			if (null != searchCriteria.getGroupIds() && searchCriteria.getGroupIds().size() > 0) {
				if (hasUserId)
					queryBuilder.append(" or ");
				queryBuilder.append("potentialOwners.id in (?").append(++nextPosition).append(")");
				Set<Object> paramVals = new HashSet<Object>();
				for (String str : searchCriteria.getGroupIds()) {
					if (null != str) {
						paramVals.add(str);
					}
				}
				queryParams.put(String.valueOf(nextPosition), paramVals);
			}
			queryBuilder
					.append(")")
					.append(
						" and (potentialOwners.id in (select pot_.entity_id from PeopleAssignments_PotOwners pot_ where t.id=pot_.task_id))");
			if (hasUserId) {
				// (t.actualOwner_id is null or t.actualOwner_id = ?)
				queryBuilder.append(" and (t.actualOwner_id = ?").append(++nextPosition)
						.append(" or t.actualOwner_id is null )");
				queryParams.put(String.valueOf(nextPosition), searchCriteria.getUserId());
			}
			if(hasUserId) {
				queryBuilder.append(" and ( ?").append(++nextPosition).append(" not in (select pxc_.entity_id from peopleassignments_exclowners pxc_ where t.id = pxc_.task_id )) ");
				queryParams.put(String.valueOf(nextPosition), searchCriteria.getUserId());
			}
			if (searchCriteria.isExcludedUser()) {
				if (null != searchCriteria.getExcludedUserIds() && searchCriteria.getExcludedUserIds().length() > 0) {
					queryBuilder
							.append(
								" and (t.id not in (select pot_.task_id from PeopleAssignments_PotOwners pot_ where t.id = pot_.task_id and entity_id in (?")
							.append(++nextPosition).append(")) or t.actualOwner_id is not null)");
					if (searchCriteria.getExcludedUserIds().indexOf(",") > 0) {
						Set<Object> paramVals = new HashSet<Object>();
						for (String str : searchCriteria.getExcludedUserIds().split(",")) {
							if (null != str) {
								paramVals.add(str);
							}
						}
						queryParams.put(String.valueOf(nextPosition), paramVals);
					} else {
						queryParams.put(String.valueOf(nextPosition), searchCriteria.getExcludedUserIds());
					}
				} else if (hasUserId) {
					queryBuilder
							.append(
								" and (t.id not in (select pot_.task_id from PeopleAssignments_PotOwners pot_ where t.id = pot_.task_id and entity_id in (?")
							.append(++nextPosition).append(")) or t.actualOwner_id is not null)");
					queryParams.put(String.valueOf(nextPosition), "!" + searchCriteria.getUserId());
				}
			}
		}

		if (null != searchCriteria.getOrderBy() && !searchCriteria.getOrderBy().isEmpty()) {
			queryBuilder.append(searchCriteria.getOrderBy());
		} else {
			queryBuilder.append(" order by t.processInstanceId DESC, t.id DESC");
		}

		String query = queryBuilder.toString();
		String totalSql = "select count(*) from (" + query + ")";
		logger.info("QUERY: " + query);
		logger.info("Query params:" + queryParams);
		logger.info("Total QUERY: " + totalSql);

		// 2014.11.3 support totalPages and totalRows
		int totalRows = persistenceContext.nativequeryTotalRows(totalSql, queryParams);
		int totalPages = (totalRows + searchCriteria.getMaxResults() - 1) / searchCriteria.getMaxResults();

		List<Object[]> list = persistenceContext.nativequeryWithParametersInTransaction(query, queryParams);
		List<TaskSummary> results = new ArrayList<TaskSummary>();
		if (null != list) {
			boolean isFirstRecord = true;
			for (Object[] row : list) {
				int i = -1;
				TaskSummaryImpl vo = new TaskSummaryImpl();
				vo.setId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setProcessInstanceId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setName((String)row[++i]);
				vo.setFormName((String) row[++i]);
				vo.setSubject((String)row[++i]);
				vo.setDescription((String)row[++i]);
				vo.setStatus(Status.valueOf((String)row[++i]));
				vo.setPriority((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue()));
				vo.setSkipable(((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue())) == 1);
				UserImpl user = new UserImpl();
				user.setId((String)row[++i]);
				vo.setActualOwner(user);
				user = new UserImpl();
				user.setId((String)row[++i]);
				vo.setCreatedBy(user);
				vo.setCreatedOn((Date)row[++i]);
				vo.setActivationTime((Date)row[++i]);
				vo.setExpirationTime((Date)row[++i]);
				vo.setProcessId((String)row[++i]);
				vo.setProcessSessionId((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue()));
				vo.setSubTaskStrategy(SubTasksStrategy.valueOf((String)row[++i]));
				vo.setParentId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setBatchProcessType((String)row[++i]);
				vo.setDeploymentId((String)row[++i]);
				// all keys must be lower case. The following statements must be
				// same as
				// the block in line 483 of JPAAuditLogService.java
				Map<String, Object> map = new HashMap<String, Object>();
				if (null != row[++i])
					map.put("site_code", row[i]);
				if (null != row[++i])
					map.put("service_code", row[i]);
				if (null != row[++i])
					map.put("company_code", row[i]);
				if (null != row[++i])
					map.put("process_group", row[i]);
				if (null != row[++i])
					map.put("item_key", row[i]);
				if (null != row[++i])
					map.put("item_type", row[i]);
				if (null != row[++i])
					map.put("opt_type", row[i]);
				if (null != row[++i])
					map.put("text1", row[i]);
				if (null != row[++i])
					map.put("text2", row[i]);
				if (null != row[++i])
					map.put("text3", row[i]);
				if (null != row[++i])
					map.put("text4", row[i]);
				if (null != row[++i])
					map.put("text5", row[i]);
				if (null != row[++i])
					map.put("char1", row[i]);
				if (null != row[++i])
					map.put("char2", row[i]);
				if (null != row[++i])
					map.put("money1", row[i]);
				if (null != row[++i])
					map.put("money2", row[i]);
				if (null != row[++i])
					map.put("money3", row[i]);
				if (null != row[++i])
					map.put("integer1", row[i]);
				if (null != row[++i])
					map.put("integer2", row[i]);
				if (null != row[++i])
					map.put("decimal1", row[i]);
				if (null != row[++i])
					map.put("decimal2", row[i]);
				if (null != row[++i])
					map.put("date1", row[i]);
				if (null != row[++i])
					map.put("date2", row[i]);
				if (null != row[++i])
					map.put("date3", row[i]);
				if (null != row[++i])
					map.put("timestamp1", row[i]);
				if (null != row[++i])
					map.put("timestamp2", row[i]);
				if (null != row[++i])
					map.put("wfe_client_id", row[i]);
				if (null != row[++i])
					map.put("Bu_Name", row[i]);
				if (isFirstRecord) {
					// only the first record holds the totalPages and totalRows
					map.put("_totalRows_", totalRows);
					map.put("_totalPages_", totalPages);
					isFirstRecord = false;
				}
				vo.setMoreProperties(map);
				results.add(vo);
			}
		}
		logger.info("Queried tasks::" + results.size());
		return results;
	}

    /**
     * Need to access the database to check the BU.
     * @param searchCriteria - SearchCriteria
     */
	private void handleBuNameParam(SearchCriteria searchCriteria) {
		String userId = searchCriteria.getUserId();
		List<String> buNames = searchCriteria.getBuNames();
		if (null == buNames || buNames.size() == 0) {
			WfeUserBuCallback buCallBack = new WfeUserBuCallback();
			boolean needCheckBUFlag = buCallBack.needCheckBU(userId,
					searchCriteria.getSiteCode(),
					searchCriteria.isWfeCheckBuFlag());
			if(needCheckBUFlag) {
				buNames = buCallBack.getBusForUser(userId,searchCriteria.getSiteCode());
				searchCriteria.setBuNames(buNames);
			}
		}
	}

	@Override
	public void updateProcessExtra(Long taskId, Map<String, Object> data) {
		persistenceContext.updateProcessExtra(taskId, data);
	}
	
	@Override
	public List<TaskSummary> getTasksByInstanceId(long processInstanceId) {
		String GENERIC_TASKSUM_QUERY = "select distinct t.id, t.processInstanceId, t.name as displayName, t.formname, t.subject, t.description, "
				+ "  t.status, t.priority, t.skipable, t.actualOwner_id, "
				+ " t.createdBy_id, t.createdOn, t.activationTime, t.expirationTime, t.processId, t.processSessionId, "
				+ " t.subTaskStrategy, t.parentId, p.EXTERNALID, propTable.site_code, propTable.service_code, propTable.company_code, "
				+ " propTable.process_group, propTable.item_key, propTable.item_type, propTable.opt_type, propTable.text1, propTable.text2, propTable.text3, propTable.text4, propTable.text5, "
				+ " propTable.char1, propTable.char2, propTable.money1, propTable.money2, propTable.money3, propTable.integer1, propTable.integer2, "
				+ "  propTable.decimal1, propTable.decimal2, propTable.date1, propTable.date2, propTable.date3, propTable.timestamp1, propTable.timestamp2, propTable.wfe_client_id "
				+ " from Task t,  "
				+ " ProcessInstanceLog p, "
				+ " ProcInstanceProp propTable "
				+ " where t.archived=0  "
				+ " and p.processInstanceId=propTable.process_instance_id and p.processInstanceId=t.processInstanceId "
				+ " and p.processInstanceId=?1 order by t.createdOn DESC ";
		Map<String, Object> queryParams = new HashMap<String, Object>();
		queryParams.put("1", processInstanceId);
		List<Object[]> list = persistenceContext.nativequeryWithParametersInTransaction(GENERIC_TASKSUM_QUERY, queryParams);
		List<TaskSummary> results = new ArrayList<TaskSummary>();
		if (null != list) {
			for (Object[] row : list) {
				int i = -1;
				TaskSummaryImpl vo = new TaskSummaryImpl();
				vo.setId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setProcessInstanceId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setName((String)row[++i]);
				vo.setFormName((String) row[++i]);
				vo.setSubject((String)row[++i]);
				vo.setDescription((String)row[++i]);
				vo.setStatus(Status.valueOf((String)row[++i]));
				vo.setPriority((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue()));
				vo.setSkipable(((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue())) == 1);
				UserImpl user = new UserImpl();
				user.setId((String)row[++i]);
				vo.setActualOwner(user);
				user = new UserImpl();
				user.setId((String)row[++i]);
				vo.setCreatedBy(user);
				vo.setCreatedOn((Date)row[++i]);
				vo.setActivationTime((Date)row[++i]);
				vo.setExpirationTime((Date)row[++i]);
				vo.setProcessId((String)row[++i]);
				vo.setProcessSessionId((null == row[++i] ? 0 : ((BigDecimal)row[i]).intValue()));
				vo.setSubTaskStrategy(SubTasksStrategy.valueOf((String)row[++i]));
				vo.setParentId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
				vo.setDeploymentId((String)row[++i]);
				Map<String, Object> map = new HashMap<String, Object>();
				if (null != row[++i])
					map.put("site_code", row[i]);
				if (null != row[++i])
					map.put("service_code", row[i]);
				if (null != row[++i])
					map.put("company_code", row[i]);
				if (null != row[++i])
					map.put("process_group", row[i]);
				if (null != row[++i])
					map.put("item_key", row[i]);
				if (null != row[++i])
					map.put("item_type", row[i]);
				if (null != row[++i])
					map.put("opt_type", row[i]);
				if (null != row[++i])
					map.put("text1", row[i]);
				if (null != row[++i])
					map.put("text2", row[i]);
				if (null != row[++i])
					map.put("text3", row[i]);
				if (null != row[++i])
					map.put("text4", row[i]);
				if (null != row[++i])
					map.put("text5", row[i]);
				if (null != row[++i])
					map.put("char1", row[i]);
				if (null != row[++i])
					map.put("char2", row[i]);
				if (null != row[++i])
					map.put("money1", row[i]);
				if (null != row[++i])
					map.put("money2", row[i]);
				if (null != row[++i])
					map.put("money3", row[i]);
				if (null != row[++i])
					map.put("integer1", row[i]);
				if (null != row[++i])
					map.put("integer2", row[i]);
				if (null != row[++i])
					map.put("decimal1", row[i]);
				if (null != row[++i])
					map.put("decimal2", row[i]);
				if (null != row[++i])
					map.put("date1", row[i]);
				if (null != row[++i])
					map.put("date2", row[i]);
				if (null != row[++i])
					map.put("date3", row[i]);
				if (null != row[++i])
					map.put("timestamp1", row[i]);
				if (null != row[++i])
					map.put("timestamp2", row[i]);
				if (null != row[++i])
					map.put("wfe_client_id", row[i]);
				vo.setMoreProperties(map);
				results.add(vo);
			}
		}
		return results;
	}

	@Override
	public List<WfTaskSummary> getTaskSummary(SearchCriteria searchCriteria) {
		String GENERIC_TASKSUM_QUERY = " select y.pk, y.createddate, y.duration, y.enddate, y.processinstanceid, y.startdate, y.status, y.taskid, y.taskname, y.userid, y.optlock, "
			+ " propTable.char1, propTable.char2, propTable.company_code, propTable.date1, propTable.date2, propTable.date3, propTable.decimal1, propTable.decimal2, propTable.integer1, propTable.integer2, propTable.item_key, propTable.item_type, propTable.money1, propTable.money2, propTable.money3, propTable.opt_type,"
			+ " propTable.process_group, propTable.service_code, propTable.site_code, propTable.text1, propTable.text2, propTable.text3, propTable.text4, propTable.text5, propTable.timestamp1, propTable.timestamp2 "
			+ " from bamtasksummary y left join procinstanceprop propTable on y.processinstanceid=propTable.process_instance_id left join processinstancelog g on y.processinstanceid = g.processinstanceid "
			+ " where g.status=2 and ";
			StringBuilder queryBuilder = new StringBuilder(GENERIC_TASKSUM_QUERY);

			Map<String, Object> queryParams = new HashMap<String, Object>();
			queryParams.put(FLUSH_MODE, FlushModeType.COMMIT.toString());
			queryParams.put(FIRST_RESULT, searchCriteria.getFirstResult());
			if (searchCriteria.getMaxResults() != -1) {
				queryParams.put(MAX_RESULTS, searchCriteria.getMaxResults());
			}

			if (null != searchCriteria.getWhereStr() && !searchCriteria.getWhereStr().isEmpty()) {
				searchCriteria.setWhereStr(searchCriteria.getWhereStr());
			}

			logger.info("searchCriteria >>>" + searchCriteria);

			int nextPosition = 0;
			if (null != searchCriteria.getWhereStr() && !searchCriteria.getWhereStr().isEmpty()) {
				queryBuilder.append(searchCriteria.getWhereStr());
				List<WhereParameter> fields = searchCriteria.getParams();
				nextPosition = fields.size();
				for (WhereParameter paramObj : fields) {
					List<?> values = paramObj.getValues();
					if (null != values) {
						if (values.size() > 1) {
							Set<Object> paramVals = new HashSet<Object>();
							for (Object val : values) {
								if (val != null) {
									paramVals.add(val);
								}
							}
							queryParams.put(String.valueOf(paramObj.getParameterPosition()), paramVals);
						} else if (values.size() == 1) {
							queryParams.put(String.valueOf(paramObj.getParameterPosition()), values.get(0));
						}
					}
				}
			}
			if (null != searchCriteria.getOrderBy() && !searchCriteria.getOrderBy().isEmpty()) {
				queryBuilder.append(searchCriteria.getOrderBy());
			} else {
				queryBuilder.append(" order by t.processInstanceId DESC, t.id DESC");
			}
			String query = queryBuilder.toString();
			String totalSql = "select count(*) from (" + query + ")";
			logger.info("QUERY: " + query);
			logger.info("Query params:" + queryParams);
			logger.info("Total QUERY: " + totalSql);

			// 2014.11.3 support totalPages and totalRows
			int totalRows = persistenceContext.nativequeryTotalRows(totalSql, queryParams);
			int totalPages = (totalRows + searchCriteria.getMaxResults() - 1) / searchCriteria.getMaxResults();

			List<Object[]> list = persistenceContext.nativequeryWithParametersInTransaction(query, queryParams);
			List<WfTaskSummary> results = new ArrayList<WfTaskSummary>();
			if (null != list) {
				boolean isFirstRecord = true;
				for (Object[] row : list) {
					int i = -1;
					WfTaskSummary vo = new WfTaskSummary();
					vo.setPk((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setCreatedDate((Date)row[++i]);
					vo.setDuration((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setEndDate((Date)row[++i]);
					vo.setProcessinstanceid((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setStartdate((Date)row[++i]);
					vo.setTaskStatus((String)row[++i]);
					vo.setTaskId((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setTaskName((String)row[++i]);
					vo.setUserId((String)row[++i]);
					vo.setOptLock((int)((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue())));
					vo.setChar1((String)row[++i]);
					vo.setChar2((String)row[++i]);
					vo.setCompanyCode((String)row[++i]);
					vo.setDate1((Date)row[++i]);
					vo.setDate2((Date)row[++i]);
					vo.setDate3((Date)row[++i]);
					vo.setDecimal1((BigDecimal)row[++i]);
					vo.setDecimal2((BigDecimal)row[++i]);
					vo.setInteger1((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setInteger2((null == row[++i] ? 0L : ((BigDecimal)row[i]).longValue()));
					vo.setItemKey((String)row[++i]);
					vo.setItemType((String)row[++i]);
					vo.setMoney1((BigDecimal)row[++i]);
					vo.setMoney2((BigDecimal)row[++i]);
					vo.setMoney3((BigDecimal)row[++i]);
					vo.setOptType((String)row[++i]);
					vo.setProcessGroup((String)row[++i]);
					vo.setServiceCode((String)row[++i]);
					vo.setSiteCode((String)row[++i]);
					vo.setText1((String)row[++i]);
					vo.setText2((String)row[++i]);
					vo.setText3((String)row[++i]);
					vo.setText4((String)row[++i]);
					vo.setText5((String)row[++i]);
					vo.setTimestamp1((Date)row[++i]);
					vo.setTimestamp2((Date)row[++i]);
					if (isFirstRecord) {
						vo.setTotalRows(totalRows);
						vo.setTotalPages(totalPages);
						isFirstRecord = false;
					}
					results.add(vo);
				}
			}
			logger.info("Queried tasks::" + results.size());
			return results;
	}
}
