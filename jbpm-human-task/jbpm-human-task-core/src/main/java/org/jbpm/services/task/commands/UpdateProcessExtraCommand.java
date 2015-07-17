package org.jbpm.services.task.commands;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.drools.core.xml.jaxb.util.JaxbMapAdapter;
import org.kie.internal.command.Context;

@XmlRootElement(name="update-process-extra-command")
@XmlAccessorType(XmlAccessType.NONE)
public class UpdateProcessExtraCommand extends UserGroupCallbackTaskCommand<Void> {

	private static final long serialVersionUID = 1L;
	
	@XmlJavaTypeAdapter(JaxbMapAdapter.class)
    @XmlElement
    protected Map<String, Object> data;
	
	public UpdateProcessExtraCommand() {
    }

    public UpdateProcessExtraCommand(long taskId, Map<String, Object> data) {
        this.taskId = taskId;
        this.data = data;
    }
    
    public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	public Void execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        // 2015.2.24 PTI update extra table
        context.getTaskQueryService().updateProcessExtra(taskId, data);
    	return null;
        
    }
	
}
