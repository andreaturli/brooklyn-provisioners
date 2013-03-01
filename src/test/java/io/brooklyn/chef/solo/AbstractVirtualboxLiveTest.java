package io.brooklyn.chef.solo;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableMap;

/**
 * Runs a test with many different distros and versions.
 */
public abstract class AbstractVirtualboxLiveTest {
    
    public static final String PROVIDER = "vbox";
    public static final String LOCATION_SPEC = PROVIDER;
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    
    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;
    
    protected TestApplication app;
    protected Location jcloudsLocation;
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-id");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");
        
        ctx = new LocalManagementContext();
        app = ApplicationBuilder.builder(TestApplication.class).manage(ctx);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }

    @Test(groups = {"Live"})
    public void test_Ubuntu_12_0() throws Exception {
        runTest(ImmutableMap.of("loginUser", "toor", "loginUser.password", "password"));
    }

    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        runTest(ImmutableMap.of("loginUser", "toor", "loginUser.password", "password"));
    }

    protected void runTest(Map<?,?> flags) throws Exception {
        Map<?,?> jcloudsFlags = MutableMap.builder()
                .putAll(flags)
                .build();
        jcloudsLocation = ctx.getLocationRegistry().resolve(LOCATION_SPEC, jcloudsFlags);

        doTest(jcloudsLocation);
    }
    
    protected abstract void doTest(Location loc) throws Exception;
}
