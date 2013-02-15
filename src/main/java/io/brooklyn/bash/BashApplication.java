package io.brooklyn.bash;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.launcher.BrooklynServerDetails;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class BashApplication extends ApplicationBuilder {

    public static final Logger LOG = LoggerFactory.getLogger(BashApplication.class);

    public static final String DEFAULT_LOCATION = "localhost";

    @Override
    protected void doBuild() {
        createChild(BasicEntitySpec.newInstance(NginxController.class));
    }

    public static void main(String[] argv) {
        
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION);

        BrooklynServerDetails server = BrooklynLauncher.newLauncher().webconsolePort(port).launch();

        Location loc = server.getManagementContext().getLocationRegistry().resolve(location);

        StartableApplication app = new BashApplication().appDisplayName(
                "Brooklyn Bash provisioner for Nginx example").manage(server.getManagementContext());

        app.start(ImmutableList.of(loc));

        Entities.dumpInfo(app);
    }

}
