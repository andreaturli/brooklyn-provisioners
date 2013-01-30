package io.brooklyn.puppet;

import static java.util.Arrays.asList;
import io.brooklyn.puppet.entities.MySqlPuppetSshDriver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.BrooklynProperties;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.drivers.BasicEntityDriverFactory;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.entity.webapp.tomcat.TomcatServer;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.location.Location;
import brooklyn.location.basic.LocationRegistry;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.basic.jclouds.JcloudsLocation;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.util.MutableMap;
import brooklyn.util.ResourceUtils;
import brooklyn.util.exceptions.Exceptions;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

@SuppressWarnings("rawtypes")
public class PuppetApplication extends AbstractApplication {

    private static final long serialVersionUID = -14811123920351157L;
    private static final Logger logger = LoggerFactory.getLogger(PuppetApplication.class);

    public static final ConfigKey<String> LOCATION_SPECS = new BasicConfigKey<String>(String.class, "application.locations",
            "Locations where Application should run (required)", null);    
    
    private static final String USEAST_REGION_NAME = "us-east-1";
    private static final String USEAST_IMAGE_ID = USEAST_REGION_NAME + "/" + "ami-2342a94a";
    
    private static final Splitter LOCATIONS_SPLITTER = Splitter.on(';')
            .trimResults()
            .omitEmptyStrings();
    
    public static final BasicAttributeSensor<Lifecycle> SERVICE_STATE = Attributes.SERVICE_STATE;
    
    private MySqlNode mysql;

    public PuppetApplication() {}
    public PuppetApplication(Map flags) { super(flags); }
   
    public boolean isDefault(ConfigKey<String> configKey) {
        return "default".equalsIgnoreCase(getConfig(configKey));
    }
    public boolean isNew(ConfigKey<String> configKey) {
        return "new".equalsIgnoreCase(getConfig(configKey));
    }
    
    @Override
    public void onManagementStarted() {
        configsInternal.setInheritedConfig(getManagementSupport().getManagementContext(false).getConfig().getAllConfig());        
        super.onManagementStarted();
    }
    
    @Override
    public void start(Collection<? extends Location> locations) {
        logger.info("Starting Application with locations {}", locations);
        try {
            setAttribute(SERVICE_STATE, Lifecycle.STARTING);
            
            initDatabase(locations);
            
            StartableMethods.start(this, locations);
            log.info("Successfully started Application");

            if (getAttribute(SERVICE_STATE) == Lifecycle.STARTING) setAttribute(SERVICE_STATE, Lifecycle.RUNNING);
            setAttribute(SERVICE_UP, true);
        } catch (Exception e) {
            logger.error("Failed to start Application "+this+": "+e, e);
            setAttribute(SERVICE_STATE, Lifecycle.ON_FIRE);
            setAttribute(SERVICE_UP, false);
            throw Exceptions.propagate(e);
        }
    }

	private void initDatabase(Collection<? extends Location> locations) {
		mysql = new MySqlNode(this);
		Entities.startManagement(mysql);
	}

    @Override
    public void restart() {
        logger.info("Restarting application");
        logger.info("Restarting application Finished");
    }

    @Override
    public void stop() {
        setAttribute(SERVICE_UP, false);
        setAttribute(SERVICE_STATE, Lifecycle.STOPPING);
        logger.info("Stopping application");
        logger.info("Stopping application Finished");
        setAttribute(SERVICE_STATE, Lifecycle.STOPPED);
    }

    public static void main(String[] args) {
        BrooklynProperties applicationBrooklynProperties = loadProperties(args);
        ManagementContext managementContext = new LocalManagementContext(applicationBrooklynProperties);       
        PuppetApplication app = buildApplication(managementContext);
        startApplication(app, managementContext, applicationBrooklynProperties);
    }
    
    private static void startApplication(PuppetApplication app, ManagementContext managementContext, BrooklynProperties appProperties) {
        String locationsString = (String) managementContext.getConfig().getConfig(LOCATION_SPECS);
        if (locationsString == null) locationsString = "";

        // Figure out the brooklyn location(s) where to launch the application
        List<String> parsedLocations = Lists.newArrayList(LOCATIONS_SPLITTER.split(locationsString));
        logger.info("Parsed user provided location(s): {}", parsedLocations);
        if (parsedLocations.isEmpty()) {
            throw new IllegalStateException("app properties must define application.locations");
        }
        List<Location> locations = new LocationRegistry(appProperties).getLocationsById(parsedLocations);

        for (Location loc : locations) {
            if ((loc instanceof JcloudsLocation) && "aws-ec2".equals(((JcloudsLocation)loc).getProvider())) {
	            JcloudsLocation jcloudsLocation = (JcloudsLocation) loc;
	            Map<String, Map<String, ? extends Object>> tagMapping = new HashMap<String, Map<String, ? extends Object>>();
	            tagMapping.put(TomcatServer.class.getName(), MutableMap.of("imageId", USEAST_IMAGE_ID, "securityGroups", asList("brooklyn-all")));
	            jcloudsLocation.setTagMapping(tagMapping);
            }
        }

        BasicEntityDriverFactory factory = (BasicEntityDriverFactory) managementContext.getEntityDriverFactory();
        factory.registerDriver(MySqlDriver.class, SshMachineLocation.class, MySqlPuppetSshDriver.class);

        // Start the application
        app.start(locations);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static PuppetApplication buildApplication(ManagementContext managementContext) {

        Map flags = MutableMap.of(
                "mgmt", managementContext,
                "displayName", "Application");

        PuppetApplication app = new PuppetApplication(flags);
        managementContext.manage(app);
        return app;
    }
    
    @SuppressWarnings("unchecked")
    private static BrooklynProperties loadProperties(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage:\n" +
                    "\tnohup start.sh /path/to/application.properties &");
            System.exit(1);
        }

        BrooklynProperties properties = BrooklynProperties.Factory.newDefault();
        properties.putAll(localProperties(args[0]));
        // add sys properties back in
        properties.addSystemProperties();

        return properties;
    }

    private static Properties localProperties(String propertiesFilePath) {
        logger.info("Loading application properties from " + propertiesFilePath);
        InputStream propsStream = new ResourceUtils(PuppetApplication.class.getClassLoader()).getResourceFromUrl(propertiesFilePath);
        Properties cloudbandProperties = new Properties();
        try {
            cloudbandProperties.load(propsStream);
            return cloudbandProperties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(propsStream);
        }
    }
}
