package org.jbpm.services.task.impl.model.xml;

import static org.jbpm.services.task.impl.model.xml.AbstractJaxbTaskObject.convertListFromInterfaceToJaxbImpl;
import static org.jbpm.services.task.impl.model.xml.AbstractJaxbTaskObject.unsupported;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jbpm.services.task.commands.AddTaskCommand;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.PeopleAssignments;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.User;
import org.kie.internal.jaxb.StringKeyObjectValueMapXmlAdapter;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.model.Deadlines;
import org.kie.internal.task.api.model.Delegation;
import org.kie.internal.task.api.model.InternalAttachment;
import org.kie.internal.task.api.model.InternalComment;
import org.kie.internal.task.api.model.InternalI18NText;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;
import org.kie.internal.task.api.model.SubTasksStrategy;

@XmlRootElement(name="task")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties({"archived","deadlines"})
@JsonAutoDetect(getterVisibility=JsonAutoDetect.Visibility.NONE, setterVisibility=JsonAutoDetect.Visibility.NONE, fieldVisibility=JsonAutoDetect.Visibility.ANY)
public class JaxbTask implements InternalTask {

    @XmlElement
    @XmlSchemaType(name="long")
    private Long id;
    
    @XmlElement
    @XmlSchemaType(name="int")
    private Integer priority;

    @XmlElement
    @XmlSchemaType(name="int")
    private Integer version;

    @XmlElement
    @XmlSchemaType(name="boolean")
    private Boolean archived;

    @XmlElement(name="task-type")
    @XmlSchemaType(name="string")
    private String taskType; 
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String name;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String subject;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String description;
    
    @XmlElement
    private List<JaxbI18NText> names;
    
    @XmlElement
    private List<JaxbI18NText> subjects;
    
    @XmlElement
    private List<JaxbI18NText> descriptions;
    
    @XmlElement(name="people-assignments")
    private JaxbPeopleAssignments peopleAssignments;
    
    @XmlElement
    private SubTasksStrategy subTasksStrategy;
    
    @XmlElement
    private JaxbTaskData taskData;
    
    @XmlElement
    private JaxbDeadlines deadlines = new JaxbDeadlines();
    /**
     * @author PTI
     */
    @XmlElement(name="form-name")
    @XmlSchemaType(name="string")
    private String formName;
    /**
     * @author PTI
     */
    @XmlElement(name="batch-process-type")
    @XmlSchemaType(name="string")
	private String batchProcessType;
    /**
     * @author PTI
     */
	@XmlElement(name = "actor-company-type")
	@XmlSchemaType(name = "string")
	private String actorCompanyType;
    /**
     * Use map to return our self-defined table properties
     * @author PTI
     */
    @XmlElement
    @XmlJavaTypeAdapter(StringKeyObjectValueMapXmlAdapter.class)
    private Map<String, Object> moreProperties = null;
    
    public JaxbTask() { 
        // Default constructor
    }
    
    public JaxbTask(Task task) { 
        initialize(task);
    }
    
    public void initialize(Task task) { 
        if( task == null ) { 
            return;
        }
        this.id = task.getId();
        this.priority = task.getPriority();
        this.subTasksStrategy = ((InternalTask) task).getSubTaskStrategy();
        this.peopleAssignments = new JaxbPeopleAssignments(task.getPeopleAssignments());

        this.names = convertListFromInterfaceToJaxbImpl(task.getNames(), I18NText.class, JaxbI18NText.class);
        this.name = ((InternalTask)task).getName();
        this.subjects = convertListFromInterfaceToJaxbImpl(task.getSubjects(), I18NText.class, JaxbI18NText.class);
        this.subject = ((InternalTask)task).getSubject();
        this.descriptions = convertListFromInterfaceToJaxbImpl(task.getDescriptions(), I18NText.class, JaxbI18NText.class);
        this.description = ((InternalTask)task).getDescription();
        
        this.taskType = task.getTaskType();
        this.formName = ((InternalTask)task).getFormName();
        this.taskData = new JaxbTaskData(task.getTaskData());
        /*
         * @author PTI
         */
		this.moreProperties = task.getMoreProperties();
		this.batchProcessType = task.getBatchProcessType();
		this.actorCompanyType = task.getActorCompanyType();
    }
   
    /**
     * This is a convienence method that retrieves a TaskImpl instance. It's used
     * internally in the {@link AddTaskCommand#execute(org.kie.internal.command.Context)} method
     * because that command requires a persistable task representation.
     * </p>
     * Users who are looking for information from the task should <i>not</i> use this method: 
     * all of the task information is already available via the normal methods
     * defined by the {@link Task} or {@link InternalTask} interfaces, both of which this class 
     * implements: for example: {@link JaxbTask#getId()}, {@link JaxbTask#getTaskData()} 
     * or {@link JaxbTask#getPeopleAssignments()}.
     * @return a TaskImpl instance
     */
    public Task getTask() { 
        InternalTask taskImpl = (InternalTask) TaskModelProvider.getFactory().newTask();

        taskImpl.setId(this.getId());
        taskImpl.setPriority(this.getPriority());
        
        JaxbPeopleAssignments jaxbPeopleAssignments = this.peopleAssignments;
        InternalPeopleAssignments peopleAssignments = (InternalPeopleAssignments) TaskModelProvider.getFactory().newPeopleAssignments();
        if (jaxbPeopleAssignments.getTaskInitiator() != null) {
            User user = createUser(this.getPeopleAssignments().getTaskInitiator().getId());
            peopleAssignments.setTaskInitiator(user);
        }
        List<OrganizationalEntity> potentialOwners = copyOrganizationalEntityList(jaxbPeopleAssignments.getPotentialOwners());
        peopleAssignments.setPotentialOwners(potentialOwners);
        List<OrganizationalEntity> businessAdmins = copyOrganizationalEntityList(jaxbPeopleAssignments.getBusinessAdministrators());
        peopleAssignments.setBusinessAdministrators(businessAdmins);
        List<OrganizationalEntity> exclOwners = copyOrganizationalEntityList(jaxbPeopleAssignments.getExcludedOwners());
        peopleAssignments.setExcludedOwners(exclOwners);
        List<OrganizationalEntity> taskStake = copyOrganizationalEntityList(jaxbPeopleAssignments.getTaskStakeholders());
        peopleAssignments.setTaskStakeholders(taskStake);
        List<OrganizationalEntity> recipients = copyOrganizationalEntityList(jaxbPeopleAssignments.getRecipients());
        peopleAssignments.setRecipients(recipients);
        taskImpl.setPeopleAssignments(peopleAssignments);        
        
        taskImpl.setSubTaskStrategy(this.getSubTaskStrategy());
      
        {
            List<I18NText> names = new ArrayList<I18NText>();
            for (I18NText n: this.getNames()) {
                I18NText text = TaskModelProvider.getFactory().newI18NText();
                ((InternalI18NText) text).setLanguage(n.getLanguage());
                ((InternalI18NText) text).setText(n.getText());
                names.add(text);
            }        
            taskImpl.setNames(names);
        }
        
        if (this.getName() != null) {
            taskImpl.setName(this.getName());   
        } else if (!this.getNames().isEmpty()) {
            taskImpl.setName(this.getNames().get(0).getText());
        }

        {
            List<I18NText> subjects = new ArrayList<I18NText>();
            for (I18NText n: this.getSubjects()) {
                I18NText text = TaskModelProvider.getFactory().newI18NText();
                ((InternalI18NText) text).setLanguage(n.getLanguage());
                ((InternalI18NText) text).setText(n.getText());
                subjects.add(text);
            }
            taskImpl.setSubjects(subjects);
        }
        if (this.getSubject() != null) {
            taskImpl.setSubject(this.getSubject()); 
        } else if (!this.getSubjects().isEmpty()) {
            taskImpl.setSubject(this.getSubjects().get(0).getText());
        }

        {
            List<I18NText> descriptions = new ArrayList<I18NText>();
            for (I18NText n: this.getDescriptions()) {
                I18NText text = TaskModelProvider.getFactory().newI18NText();
                ((InternalI18NText) text).setLanguage(n.getLanguage());
                ((InternalI18NText) text).setText(n.getText());
                descriptions.add(text);
            }
            taskImpl.setDescriptions(descriptions);
        }
        if (this.getDescription() != null) {
            taskImpl.setDescription(this.getDescription()); 
        } else if (!this.getDescriptions().isEmpty()) {
            taskImpl.setDescription(this.getDescriptions().get(0).getText());
        }
       
        taskImpl.setTaskType(this.getTaskType());
        taskImpl.setFormName(this.getFormName());
        /*
         * @author PTI
         */
        taskImpl.setBatchProcessType(this.getBatchProcessType());
        taskImpl.setActorCompanyType(this.getActorCompanyType());
        
        // task data
        InternalTaskData taskData = (InternalTaskData) TaskModelProvider.getFactory().newTaskData();
        JaxbTaskData jaxbTaskData = (JaxbTaskData) this.getTaskData();
        taskData.setStatus(jaxbTaskData.getStatus());
        taskData.setPreviousStatus(jaxbTaskData.getPreviousStatus());
        taskData.setActualOwner(createUser(jaxbTaskData.getActualOwnerId()));
        taskData.setCreatedBy(createUser(jaxbTaskData.getCreatedById()));
        taskData.setCreatedOn(jaxbTaskData.getCreatedOn());
        taskData.setActivationTime(jaxbTaskData.getActivationTime());
        taskData.setExpirationTime(jaxbTaskData.getExpirationTime());
        taskData.setSkipable(jaxbTaskData.isSkipable());
        taskData.setWorkItemId(jaxbTaskData.getWorkItemId());
        taskData.setProcessInstanceId(jaxbTaskData.getProcessInstanceId());
        taskData.setDocumentContentId(jaxbTaskData.getDocumentContentId());
        taskData.setDocumentAccessType(jaxbTaskData.getDocumentAccessType());
        taskData.setDocumentType(jaxbTaskData.getDocumentType());
        taskData.setOutputAccessType(jaxbTaskData.getOutputAccessType());
        taskData.setOutputType(jaxbTaskData.getOutputType());
        taskData.setOutputContentId(jaxbTaskData.getOutputContentId());
        taskData.setFaultName(jaxbTaskData.getFaultName());
        taskData.setFaultAccessType(jaxbTaskData.getFaultAccessType());
        taskData.setFaultType(jaxbTaskData.getFaultType());
        taskData.setFaultContentId(jaxbTaskData.getFaultContentId());
        taskData.setParentId(jaxbTaskData.getParentId());
        taskData.setProcessId(jaxbTaskData.getProcessId());
        taskData.setProcessSessionId(jaxbTaskData.getProcessSessionId());
        
        List<Comment> jaxbComments = jaxbTaskData.getComments();
        if( jaxbComments != null ) { 
            List<Comment> comments = new ArrayList<Comment>(jaxbComments.size());
            for( Comment jaxbComment : jaxbComments ) { 
                InternalComment comment = (InternalComment) TaskModelProvider.getFactory().newComment();
                comment.setId(jaxbComment.getId());
                comment.setAddedAt(jaxbComment.getAddedAt());
                comment.setAddedBy(createUser(((JaxbComment) jaxbComment).getAddedById()));
                comment.setText(jaxbComment.getText());
                comments.add(comment);
            }
            taskData.setComments(comments);
        }
        List<Attachment> jaxbAttachments = jaxbTaskData.getAttachments();
        if( jaxbAttachments != null ) { 
            List<Attachment> attachments = new ArrayList<Attachment>(jaxbAttachments.size());
            for( Attachment jaxbAttach : jaxbAttachments ) { 
                InternalAttachment attach = (InternalAttachment) TaskModelProvider.getFactory().newAttachment();
                attach.setId(jaxbAttach.getId());
                attach.setName(jaxbAttach.getName());
                attach.setContentType(jaxbAttach.getContentType());
                attach.setAttachedAt(jaxbAttach.getAttachedAt());
                attach.setAttachedBy(createUser(((JaxbAttachment) jaxbAttach).getAttachedById()));
                attach.setSize(jaxbAttach.getSize());
                attach.setAttachmentContentId(jaxbAttach.getAttachmentContentId());
                attachments.add(attach);
            }
            taskData.setAttachments(attachments);
        }
        taskData.setDeploymentId(jaxbTaskData.getDeploymentId());
        
        ((InternalTask)taskImpl).setTaskData(taskData);
        
        return taskImpl;
    }
   
    private User createUser(String userId) { 
        if( userId == null ) { 
            return null;
        }
        return TaskModelProvider.getFactory().newUser(userId);
    }
    
    private Group createGroup(String groupId) { 
        if( groupId == null ) { 
            return null;
        }
        return TaskModelProvider.getFactory().newGroup(groupId);
    }
    
    private List<OrganizationalEntity> copyOrganizationalEntityList(List<OrganizationalEntity> jaxbOrgEntList) { 
        if( jaxbOrgEntList == null ) { 
            return null;
        }
        List<OrganizationalEntity> orgEntList = new ArrayList<OrganizationalEntity>(jaxbOrgEntList.size());
        for( OrganizationalEntity jaxbOrgEnt : jaxbOrgEntList ) { 
            if(jaxbOrgEnt instanceof User) {
                User user = createUser(jaxbOrgEnt.getId());
                orgEntList.add(user);
            } else if(jaxbOrgEnt instanceof Group) {
                Group group = createGroup(jaxbOrgEnt.getId());
                orgEntList.add(group);
            } 
        }
        return orgEntList;
    }
    
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public List<I18NText> getNames() {
        if( names == null ) { 
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(JaxbI18NText.convertListFromJaxbImplToInterface(names));
    }

    public void setNames(List<I18NText> names) {
        this.names = convertListFromInterfaceToJaxbImpl(names, I18NText.class, JaxbI18NText.class);
    }

    public List<I18NText> getSubjects() {
        if( subjects == null ) { 
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(JaxbI18NText.convertListFromJaxbImplToInterface(subjects));
    }

    public void setSubjects(List<I18NText> subjects) {
        this.subjects = convertListFromInterfaceToJaxbImpl(subjects, I18NText.class, JaxbI18NText.class);
    }

    @Override
    public List<I18NText> getDescriptions() {
        if( descriptions == null ) { 
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(JaxbI18NText.convertListFromJaxbImplToInterface(descriptions));
    }

    public void setDescriptions(List<I18NText> descriptions) {
        this.descriptions = convertListFromInterfaceToJaxbImpl(descriptions, I18NText.class, JaxbI18NText.class);
    }

    @Override
    public PeopleAssignments getPeopleAssignments() {
        return peopleAssignments;
    }

    public void setPeopleAssignments(PeopleAssignments peopleAssignments) {
        if( peopleAssignments instanceof JaxbPeopleAssignments ) { 
        this.peopleAssignments = (JaxbPeopleAssignments) peopleAssignments;
        } else { 
            this.peopleAssignments = new JaxbPeopleAssignments(peopleAssignments);
        }
    }

    @Override
    public TaskData getTaskData() {
        return taskData;
    }

    public void setTaskData(TaskData taskData) {
        if( taskData instanceof JaxbTaskData ) { 
            this.taskData = (JaxbTaskData) taskData;
        } else { 
            this.taskData = new JaxbTaskData(taskData);
        }
    }

    @Override
    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    @Override
    public Deadlines getDeadlines() {
        return this.deadlines;
    }

    @Override
    public void setDeadlines(Deadlines deadlines) {
        // no-op
    }

   
    @Override
    public void setFormName(String formName) {
        this.formName = formName;
    }

    @Override
    public String getFormName() {
        return this.formName;
    }

    @Override
    public Boolean isArchived() {
        return this.archived;
    }

    @Override
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public void setVersion(Integer version) {
        unsupported(Task.class);
    }

    @Override
    public int getVersion() {
        return (Integer) unsupported(Task.class);
    }

    @Override
    public Delegation getDelegation() {
        return (Delegation) unsupported(Task.class);
    }

    @Override
    public void setDelegation(Delegation delegation) {
        unsupported(Task.class);
    }

    @Override
    public SubTasksStrategy getSubTaskStrategy() {
        return this.subTasksStrategy;
    }

    @Override
    public void setSubTaskStrategy(SubTasksStrategy subTaskStrategy) {
        this.subTasksStrategy = subTaskStrategy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        unsupported(Task.class);
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        unsupported(Task.class);
    }

    /**
     * @author PTI
     */
    @Override
	public Map<String, Object> getMoreProperties() {
		return moreProperties;
	}
	/**
     * @author PTI
     */
    @Override
	public void setMoreProperties(Map<String, Object> moreProperties) {
		this.moreProperties = moreProperties;
	}
	
    /**
     * @author PTI
     */
    @Override
	public String getBatchProcessType() {
		return batchProcessType;
	}
    /**
     * @author PTI
     */
    @Override
	public void setBatchProcessType(String batchProcessType) {
		this.batchProcessType = batchProcessType;
	}

    /**
     * @author PTI
     */
    @Override
	public String getActorCompanyType() {
		return actorCompanyType;
	}

    /**
     * @author PTI
     */
    @Override
	public void setActorCompanyType(String actorCompanyType) {
		this.actorCompanyType = actorCompanyType;
	}
}
