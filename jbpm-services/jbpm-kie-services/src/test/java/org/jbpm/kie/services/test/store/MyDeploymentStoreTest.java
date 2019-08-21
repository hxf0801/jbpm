package org.jbpm.kie.services.test.store;

import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorManager;
import org.junit.Test;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

public class MyDeploymentStoreTest {
	private static final Logger logger = LoggerFactory.getLogger(MyDeploymentStoreTest.class);
	
	@Test
	public void testEnableAndGetActiveDeployments() {
		System.setProperty("org.kie.deployment.desc.location",
				"file:src/test/resources/deployment/my-kie-deployment-descriptor.xml");
		DeploymentDescriptorManager manager = new DeploymentDescriptorManager();
		DeploymentDescriptor deploymentDescriptor = manager.getDefaultDescriptor();
		KModuleDeploymentUnit unit = new KModuleDeploymentUnit("org.jbpm", "test", "1.0");
		unit.setDeploymentDescriptor(deploymentDescriptor);
		
		logger.info("DeploymentDescriptor: " + deploymentDescriptor.toXml());
		
		XStream xstream = new XStream();
		String unitContent = xstream.toXML(unit);
		logger.info("DeploymentStore desc: " + unitContent);
	}

}
