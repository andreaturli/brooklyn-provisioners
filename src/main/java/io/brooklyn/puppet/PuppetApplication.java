package io.brooklyn.puppet;

import io.brooklyn.puppet.entities.MySqlPuppetSshDriver;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.drivers.BasicEntityDriverFactory;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.launcher.BrooklynServerDetails;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class PuppetApplication extends ApplicationBuilder {

    public static final Logger LOG = LoggerFactory.getLogger(PuppetApplication.class);

    public static final String DEFAULT_LOCATION = "localhost";

    @Override
    protected void doBuild() {
        createChild(BasicEntitySpec.newInstance(MySqlNode.class));
    }

    public static void main(String[] argv) {
        
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION);

        BrooklynServerDetails server = BrooklynLauncher.newLauncher().webconsolePort(port).launch();

        Location loc = server.getManagementContext().getLocationRegistry().resolve(location);

        BasicEntityDriverFactory factory = (BasicEntityDriverFactory) server.getManagementContext()
                                                                            .getEntityDriverFactory();
        factory.registerDriver(MySqlDriver.class, SshMachineLocation.class, MySqlPuppetSshDriver.class);

        StartableApplication app = new PuppetApplication().appDisplayName(
                "Brooklyn Puppet Masterless integration with MySql example").manage(server.getManagementContext());

        app.start(ImmutableList.of(loc));

        Entities.dumpInfo(app);
    }

}
