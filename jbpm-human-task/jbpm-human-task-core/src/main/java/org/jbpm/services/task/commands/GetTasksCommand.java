package org.jbpm.services.task.commands;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.api.search.SearchCriteria;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.command.Context;

/**
 * Generic custom task query
 * @author PTI
 */
@XmlRootElement(name = "get-tasks-command")
@XmlAccessorType(XmlAccessType.NONE)
public class GetTasksCommand extends UserGroupCallbackTaskCommand<List<TaskSummary>> {
	private static final long serialVersionUID = -7588660203940682629L;
	@XmlElement
	private SearchCriteria searchCriteria;

	public GetTasksCommand() {
	}

	public GetTasksCommand(SearchCriteria searchCriteria) {
		this.searchCriteria = searchCriteria;
	}

	@Override
	public List<TaskSummary> execute(Context cntxt) {
		TaskContext context = (TaskContext) cntxt;
	
		String userId = this.searchCriteria.getUserId();
		List<String> groupIds = this.searchCriteria.getGroupIds();
		if (null != groupIds && groupIds.size() > 0) {
			doCallbackUserOperation(userId, context);
			doCallbackGroupsOperation(userId, groupIds, context);
		} else {
			groupIds = doUserGroupCallbackOperation(userId, null, context);
			if(null != groupIds) searchCriteria.setGroupIds(groupIds);
		}
		
		return context.getTaskQueryService().getTasks(searchCriteria);
	}

	public SearchCriteria getSearchCriteria() {
		return searchCriteria;
	}

	public void setSearchCriteria(SearchCriteria searchCriteria) {
		this.searchCriteria = searchCriteria;
	}

}