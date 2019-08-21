package org.jbpm.runtime.manager.impl.deploy;




import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.kie.internal.runtime.conf.AuditMode;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.conf.NamedObjectModel;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.internal.runtime.conf.PersistenceMode;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyDeploymentDescriptorTest {

	private static final Logger logger = LoggerFactory.getLogger(MyDeploymentDescriptorTest.class);

	@Test
	public void testWriteDeploymentDescriptorXml() {
		DeploymentDescriptor descriptor = new DeploymentDescriptorImpl("org.jbpm.domain");
		descriptor.getBuilder()
		.addMarshalingStrategy(
			new ObjectModel("org.jbpm.testCustomStrategy",
				new Object[]{
				new ObjectModel("java.lang.String", new Object[]{"param1"}),
			"param2"}))
			.addRequiredRole("experts");

		String deploymentDescriptorXml = descriptor.toXml();
		assertNotNull(deploymentDescriptorXml);
		logger.info(deploymentDescriptorXml);

		ByteArrayInputStream stream = new ByteArrayInputStream(deploymentDescriptorXml.getBytes());
		DeploymentDescriptor fromXml = DeploymentDescriptorIO.fromXml(stream);

		assertNotNull(fromXml);
		assertEquals("org.jbpm.domain", fromXml.getPersistenceUnit());
		assertEquals("org.jbpm.domain", fromXml.getAuditPersistenceUnit());
		assertEquals(AuditMode.JPA, fromXml.getAuditMode());
		assertEquals(PersistenceMode.JPA, fromXml.getPersistenceMode());
		assertEquals(RuntimeStrategy.SINGLETON, fromXml.getRuntimeStrategy());
		assertEquals(1, fromXml.getMarshallingStrategies().size());
		assertEquals(0, fromXml.getConfiguration().size());
		assertEquals(0, fromXml.getEnvironmentEntries().size());
		assertEquals(0, fromXml.getEventListeners().size());
		assertEquals(0, fromXml.getGlobals().size());
		assertEquals(0, fromXml.getTaskEventListeners().size());
		assertEquals(0, fromXml.getWorkItemHandlers().size());
		assertEquals(1, fromXml.getRequiredRoles().size());
	}

	@Test
	public void testReadDeploymentDescriptorMSFromXml() throws Exception {
		InputStream input = this.getClass().getResourceAsStream("/deployment/my-kie-deployment-descriptor.xml");

		DeploymentDescriptor descriptor = DeploymentDescriptorIO.fromXml(input);
		assertNotNull(descriptor);
		assertEquals("org.jbpm.domain", descriptor.getPersistenceUnit());
		assertEquals("org.jbpm.domain", descriptor.getAuditPersistenceUnit());
		assertEquals(AuditMode.JPA, descriptor.getAuditMode());
		assertEquals(PersistenceMode.JPA, descriptor.getPersistenceMode());
		assertEquals(RuntimeStrategy.PER_PROCESS_INSTANCE, descriptor.getRuntimeStrategy());
		assertEquals(0, descriptor.getMarshallingStrategies().size());
		assertEquals(0, descriptor.getConfiguration().size());
		assertEquals(0, descriptor.getEnvironmentEntries().size());
		assertEquals(1, descriptor.getEventListeners().size());
		assertEquals(0, descriptor.getGlobals().size());
		assertEquals(0, descriptor.getTaskEventListeners().size());
		assertEquals(2, descriptor.getWorkItemHandlers().size());
		assertEquals(0, descriptor.getRequiredRoles().size());
		logger.info("testReadDeploymentDescriptorMSFromXml: " + descriptor.toXml());
	}

	@Test
	public void testPrintDescriptor() {
		DeploymentDescriptor descriptor = new DeploymentDescriptorImpl("org.jbpm.domain");

		descriptor.getBuilder()
		.addWorkItemHandler(new NamedObjectModel("mvel", "Log", "new org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler()"))
		.addWorkItemHandler(new NamedObjectModel("mvel", "WebService", "new org.jbpm.process.workitem.webservice.WebServiceWorkItemHandler(ksession)"))
		.addWorkItemHandler(new NamedObjectModel("mvel", "Rest", "new org.jbpm.process.workitem.rest.RESTWorkItemHandler()"))
		.addWorkItemHandler(new NamedObjectModel("mvel", "Service Task", "new org.jbpm.process.workitem.bpmn2.ServiceTaskHandler(ksession)"));

		logger.info("testPrintDescriptor: " + descriptor.toXml());
	}

	@Test
	public void testWriteDeploymentDescriptorXmlWithDuplicateNamedObjects() {
		DeploymentDescriptor descriptor = new DeploymentDescriptorImpl("org.jbpm.domain");

		descriptor.getBuilder()
		.addWorkItemHandler(new NamedObjectModel("mvel", "Log", "new org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler()"))
		.addWorkItemHandler(new NamedObjectModel("mvel", "Log", "new org.jbpm.process.instance.impl.demo.CustomSystemOutWorkItemHandler()"))
		.addRequiredRole("experts");

		String deploymentDescriptorXml = descriptor.toXml();
		assertNotNull(deploymentDescriptorXml);
		logger.info(deploymentDescriptorXml);

		ByteArrayInputStream stream = new ByteArrayInputStream(deploymentDescriptorXml.getBytes());
		DeploymentDescriptor fromXml = DeploymentDescriptorIO.fromXml(stream);

		assertNotNull(fromXml);
		assertEquals("org.jbpm.domain", fromXml.getPersistenceUnit());
		assertEquals("org.jbpm.domain", fromXml.getAuditPersistenceUnit());
		assertEquals(AuditMode.JPA, fromXml.getAuditMode());
		assertEquals(PersistenceMode.JPA, fromXml.getPersistenceMode());
		assertEquals(RuntimeStrategy.SINGLETON, fromXml.getRuntimeStrategy());
		assertEquals(0, fromXml.getMarshallingStrategies().size());
		assertEquals(0, fromXml.getConfiguration().size());
		assertEquals(0, fromXml.getEnvironmentEntries().size());
		assertEquals(0, fromXml.getEventListeners().size());
		assertEquals(0, fromXml.getGlobals().size());
		assertEquals(0, fromXml.getTaskEventListeners().size());
		assertEquals(1, fromXml.getWorkItemHandlers().size());
		assertEquals(1, fromXml.getRequiredRoles().size());
	}

}
