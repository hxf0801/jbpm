package org.jbpm.persistence;

import java.util.List;

import org.drools.persistence.PersistenceContext;
import org.jbpm.persistence.correlation.CorrelationKeyInfo;
import org.jbpm.persistence.processinstance.ProcessInstanceExtra;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.kie.internal.process.CorrelationKey;

public interface ProcessPersistenceContext
    extends
    PersistenceContext {

    ProcessInstanceInfo persist(ProcessInstanceInfo processInstanceInfo);
    
    CorrelationKeyInfo persist(CorrelationKeyInfo correlationKeyInfo);
    
    ProcessInstanceInfo findProcessInstanceInfo(Long processId);
    
    void remove(ProcessInstanceInfo processInstanceInfo);

    List<Long> getProcessInstancesWaitingForEvent(String type);
    
    Long getProcessInstanceByCorrelationKey(CorrelationKey correlationKey);
    
    /**
     * Persist the custom table
     * @param processInstanceExtra
     * @return ProcessInstanceExtra
     * @author PTI
     */
	ProcessInstanceExtra persist(ProcessInstanceExtra processInstanceExtra);
}
