package io.brooklyn.chef.solo;

import io.brooklyn.puppet.entities.MySqlPuppetSshDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.drivers.BasicEntityDriverFactory;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.HttpTestUtils;

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
        HttpTestUtils.assertHttpStatusCodeEventuallyEquals(mysql.getAttribute(NginxController.ROOT_URL), 404);
    }
    
}
