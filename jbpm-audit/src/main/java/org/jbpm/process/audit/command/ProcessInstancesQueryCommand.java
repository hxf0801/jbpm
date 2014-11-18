package org.jbpm.process.audit.command;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jbpm.process.audit.AuditLogService;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.search.SearchCriteria;
import org.kie.internal.command.Context;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessInstancesQueryCommand extends AuditCommand<List<ProcessInstanceLog>>{
	/** generated serial version UID */
	private static final long serialVersionUID = -4811721983055828869L;

	@XmlElement
    private SearchCriteria searchCriteria;
   
    public ProcessInstancesQueryCommand() {
        // JAXB constructor
    }
    
    public ProcessInstancesQueryCommand(SearchCriteria searchCriteria) {
       this.searchCriteria = searchCriteria; 
    }
    
    @Override
    public List<ProcessInstanceLog> execute( Context context ) {
        setLogEnvironment(context);
        return this.auditLogService.getProcessInstances(searchCriteria);
    }

    public SearchCriteria getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(SearchCriteria searchCriteria) {
        this.searchCriteria = searchCriteria;
    }
    
    public String toString() {
		return AuditLogService.class.getSimpleName() + ".findProcessInstances(" + searchCriteria + ")";
    }
}
