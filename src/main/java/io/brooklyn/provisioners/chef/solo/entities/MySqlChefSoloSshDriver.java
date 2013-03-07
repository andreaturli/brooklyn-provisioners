package io.brooklyn.provisioners.chef.solo.entities;

import static brooklyn.entity.basic.lifecycle.CommonCommands.sudo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Lists;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNodeImpl;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;

import com.google.common.collect.ImmutableMap;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */

public class MySqlChefSoloSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

    public static final Logger log = LoggerFactory.getLogger(MySqlChefSoloSshDriver.class);
    
    private static final String DEFAULT_FILE_CACHE_PATH = "/tmp/chef-solo";
    private static final String DEFAULT_COOKBOOKS_PATH = DEFAULT_FILE_CACHE_PATH + "/cookbooks/";
    private static final String DEFAULT_SOLO_RB = DEFAULT_FILE_CACHE_PATH + "/solo.rb";

    public MySqlChefSoloSshDriver(MySqlNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
        String cookbook = "mysql";
        String opscodeRepositoryTemplate = "https://github.com/opscode-cookbooks/%s.git";
        String buildEssentialCookbookRepository = String.format(opscodeRepositoryTemplate, "build-essential");
        String opensslCookbookRepository = String.format(opscodeRepositoryTemplate, "openssl");
        String mysqlCookbookRepository = String.format(opscodeRepositoryTemplate, "mysql");
        
        List<String> commands = installChefClientAndChefSolo();
        commands.add(configureChefSolo());
        commands.add(configureCookbookForChefSolo(cookbook));
        commands.addAll(installCookbookAndItsDependencies(ImmutableMap.of(
                                    "build-essential", createURI(buildEssentialCookbookRepository),
                                    "openssl", createURI(opensslCookbookRepository),
                                    "mysql", createURI(mysqlCookbookRepository))));
        commands.add(runChefSolo(cookbook));
        newScript(LAUNCHING).body.append(commands).failOnNonZeroResultCode().execute();
        
        
        /*
        ImmutableList.of(
                ImmutableList.of(createPpFileOnInstallDir(),
                applyPuppetModule());

        

        // configure mysql properties
        String server_root_password = "password";
        String server_repl_password = "password";
        String server_debian_password = "password";
        
        // build installation commands
        List<String> commands1 = installChefClientAndChefSolo();
        commands1.add(CommonCommands.installPackage("git"));
        commands1.add(sudo(String.format("git clone %s %s", buildEssentialCookbookRepository, buildEssentialCookbookPath)));        
        commands1.add(sudo(String.format("git clone %s %s", opensslCookbookRepository, opensslCookbookPath)));
        commands1.add(sudo(String.format("git clone %s %s", mysqlCookbookRepository, mysqlCookbookPath)));
        
        // configure chef-solo
        commands1.add("echo 'file_cache_path \"" + fileCachePath + "\"' >> " + solo_rb + " && " + 
                     "echo 'cookbook_path \""   + cookbooksPath + "\"' >> " + solo_rb + " ; ");
        // configure mysql for chef-solo
        commands1.add("echo '" + 
                     "{" + "\"mysql\": { " + 
                     "      \"server_root_password\": \"" + server_root_password + "\"," + 
                     "      \"server_repl_password\": \"" + server_repl_password + "\"," + 
                     "      \"server_debian_password\": \"" + server_debian_password + "\"" + "}, " + 
                     "      \"run_list\":[\"recipe[mysql::server]\"] " + 
                     "}' >> " + mysqlCookbookJson + "; ");
        
        // run chef-solo
        commands1.add(sudo(String.format("chef-solo -c %s -j %s; ", solo_rb, mysqlCookbookJson)));
        */
    }

    private URI createURI(String repository) {
        try {
            return new URI(repository);
        } catch (URISyntaxException e) {
            throw Exceptions.propagate(e);
        }
    }

    private String runChefSolo(String cookbook) {
        return sudo(String.format("chef-solo -c %s -j %s.json; ", DEFAULT_SOLO_RB, cookbook));
    }

    private String configureCookbookForChefSolo(String cookbook) {
        // configure mysql properties
        String server_root_password = "password";
        String server_repl_password = "password";
        String server_debian_password = "password";
        return "echo '" + 
                "{" + "\"mysql\": { " + 
                "      \"server_root_password\": \"" + server_root_password + "\"," + 
                "      \"server_repl_password\": \"" + server_repl_password + "\"," + 
                "      \"server_debian_password\": \"" + server_debian_password + "\"" + "}, " + 
                "      \"run_list\":[\"recipe[mysql::server]\"] " + 
                "}' >> " + cookbook + ".json ; ";
    }

    private String configureChefSolo() {
        return String.format("mkdir -p %s && echo 'file_cache_path \"%s\"' >> %s && " + 
               "echo 'cookbook_path \"%s\"' >> %s ;", DEFAULT_FILE_CACHE_PATH, DEFAULT_FILE_CACHE_PATH, DEFAULT_SOLO_RB, DEFAULT_COOKBOOKS_PATH, DEFAULT_SOLO_RB);
    }

    private List<String> installCookbookAndItsDependencies(Map<String, URI> dependencies) {
        List<String> commands = Lists.newArrayList();    
        commands.add(CommonCommands.installPackage("git"));
        for (String dependencyName : dependencies.keySet()) {
            String dependencyRepository = dependencies.get(dependencyName).toASCIIString();
            String dependencyCookbookPath = DEFAULT_COOKBOOKS_PATH + dependencyName;
            commands.add(String.format("git clone %s %s", dependencyRepository, dependencyCookbookPath));            
        }
        return commands;
    }

    /**
     * NB: chef-solo command is packaged together with chef-client.
     * 
     * @return list of commands
     */
    public static List<String> installChefClientAndChefSolo() {
        List<String> commands = new LinkedList<String>();
        commands.add(CommonCommands.INSTALL_CURL);
        
        commands.add("sudo true && curl -L https://www.opscode.com/chef/install.sh | sudo bash;");
        return commands;
    }
    
    @Override
    public void customize() {
        log.debug("Nothing to customize");
    }

    @Override
    public void launch() {
        log.info(String.format("Starting entity %s at %s", this, getLocation()));
        // nothing to do as mysql recipe starts mysqld
    }

    @Override
    public boolean isRunning() {
        return true;
        // return newScript(CHECK_RUNNING).body.append(callPgctl("status", false)).execute() == 0;
    }

    @Override
    public void stop() {
        // newScript(STOPPING).body.append(callPgctl("stop", false)).failOnNonZeroResultCode().execute();
    }

    @Override
    public MySqlNodeImpl getEntity() {
        return (MySqlNodeImpl) super.getEntity();
    }

    @Override
    public String getStatusCmd() {
        return "ps aux | grep [m]ysql";
    }
}