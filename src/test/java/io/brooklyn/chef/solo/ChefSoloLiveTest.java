package io.brooklyn.chef.solo;

import io.brooklyn.chef.solo.entities.MySqlChefSoloSshDriver;

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

public class ChefSoloLiveTest extends AbstractVirtualboxLiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(ChefSoloLiveTest.class);

    private MySqlNode mysql;

    @Override
    protected void doTest(Location loc) throws Exception {

        BasicEntityDriverFactory factory = (BasicEntityDriverFactory) ctx.getEntityDriverFactory();
        factory.registerDriver(MySqlDriver.class, SshMachineLocation.class, MySqlChefSoloSshDriver.class);

        mysql = app.createAndManageChild(BasicEntitySpec.newInstance(MySqlNode.class));
        app.start(ImmutableList.of(loc));

        // nginx should be up, and URL reachable
        EntityTestUtils.assertAttributeEqualsEventually(mysql, SoftwareProcess.SERVICE_UP, true);
        HttpTestUtils.assertHttpStatusCodeEventuallyEquals(mysql.getAttribute(NginxController.ROOT_URL), 404);
    }

    @Test(enabled = false)
    public void testDummy() {
    } // Convince testng IDE integration that this really does have test methods

    /*
     * @Override protected void doBuild() {
     * createChild(BasicEntitySpec.newInstance(MySqlNode.class)); }
     * 
     * public static void main(String[] argv) { List<String> args =
     * Lists.newArrayList(argv); String port =
     * CommandLineUtil.getCommandLineOption(args, "--port", "8081+"); String
     * location = CommandLineUtil.getCommandLineOption(args, "--location",
     * DEFAULT_LOCATION);
     * 
     * BrooklynServerDetails server =
     * BrooklynLauncher.newLauncher().webconsolePort(port).launch(); Location
     * loc =
     * server.getManagementContext().getLocationRegistry().resolve(location);
     * 
     * BasicEntityDriverFactory factory = (BasicEntityDriverFactory)
     * server.getManagementContext() .getEntityDriverFactory();
     * factory.registerDriver(MySqlDriver.class, SshMachineLocation.class,
     * MySqlChefSoloSshDriver.class); StartableApplication app = new
     * ChefSoloApplication().appDisplayName(
     * "Brooklyn Chef Solo integration with MySql example"
     * ).manage(server.getManagementContext());
     * app.start(ImmutableList.of(loc)); Entities.dumpInfo(app); }
     */
}
