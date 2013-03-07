package io.brooklyn.provisioners.puppet.masterless;

import io.brooklyn.provisioners.AbstractVirtualboxLiveTest;
import io.brooklyn.provisioners.puppet.entities.MySqlPuppetSshDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.drivers.BasicEntityDriverFactory;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.test.EntityTestUtils;

import com.google.common.collect.ImmutableList;

@Test
public class PuppetLiveTest extends AbstractVirtualboxLiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(PuppetLiveTest.class);
    private MySqlNode mysql;

    @Override
    protected void doTest(Location loc) throws Exception {
        BasicEntityDriverFactory factory = (BasicEntityDriverFactory) ctx.getEntityDriverFactory();
        factory.registerDriver(MySqlDriver.class, SshMachineLocation.class, MySqlPuppetSshDriver.class);

        mysql = app.createAndManageChild(BasicEntitySpec.newInstance(MySqlNode.class));
        app.start(ImmutableList.of(loc));

        EntityTestUtils.assertAttributeEqualsEventually(mysql, SoftwareProcess.SERVICE_UP, true);
    }
    
}
