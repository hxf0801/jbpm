package org.jbpm.services.task.commands;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.api.task.model.TaskSummary;
import org.kie.internal.command.Context;

/**
 * Generic custom task query
 * @author PTI
 */
@XmlRootElement(name = "get-tasks-by-instance-id-command")
@XmlAccessorType(XmlAccessType.NONE)
public class GetTasksByInstanceIdCommand extends UserGroupCallbackTaskCommand<List<TaskSummary>> {
	private static final long serialVersionUID = -7588660203940682629L;
	@XmlElement
	private long processInstanceId;

	public GetTasksByInstanceIdCommand() {
	}

	public GetTasksByInstanceIdCommand(long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	@Override
	public List<TaskSummary> execute(Context cntxt) {
		TaskContext context = (TaskContext) cntxt;
		return context.getTaskQueryService().getTasksByInstanceId(processInstanceId);
	}

	public long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

}