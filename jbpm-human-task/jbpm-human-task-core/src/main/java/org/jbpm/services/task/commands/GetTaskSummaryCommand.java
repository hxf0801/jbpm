package org.jbpm.services.task.commands;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.api.search.SearchCriteria;
import org.kie.internal.command.Context;

import com.pti.fsc.common.wf.WfTaskSummary;

/**
 * Generic custom task query
 * @author PTI
 */
@XmlRootElement(name = "get-task-summary-command")
@XmlAccessorType(XmlAccessType.NONE)
public class GetTaskSummaryCommand extends UserGroupCallbackTaskCommand<List<WfTaskSummary>> {
	private static final long serialVersionUID = -7588660203940682629L;
	@XmlElement
	private SearchCriteria searchCriteria;

	public GetTaskSummaryCommand() {
	}

	public GetTaskSummaryCommand(SearchCriteria searchCriteria) {
		this.searchCriteria = searchCriteria;
	}

	@Override
	public List<WfTaskSummary> execute(Context cntxt) {
		TaskContext context = (TaskContext) cntxt;
		/*String userId = this.searchCriteria.getUserId();
		List<String> groupIds = this.searchCriteria.getGroupIds();
		if (null != groupIds && groupIds.size() > 0) {
			doCallbackUserOperation(userId, context);
			doCallbackGroupsOperation(userId, groupIds, context);
		} else {
			groupIds = doUserGroupCallbackOperation(userId, null, context);
			if(null != groupIds) searchCriteria.setGroupIds(groupIds);
		}*/
		return context.getTaskQueryService().getTaskSummary(searchCriteria);
	}

	public SearchCriteria getSearchCriteria() {
		return searchCriteria;
	}

	public void setSearchCriteria(SearchCriteria searchCriteria) {
		this.searchCriteria = searchCriteria;
	}

}