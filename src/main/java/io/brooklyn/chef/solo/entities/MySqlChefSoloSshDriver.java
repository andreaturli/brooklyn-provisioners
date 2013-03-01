package io.brooklyn.chef.solo.entities;

import static brooklyn.entity.basic.lifecycle.CommonCommands.sudo;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNodeImpl;
import brooklyn.location.basic.SshMachineLocation;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */

public class MySqlChefSoloSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

    public static final Logger log = LoggerFactory.getLogger(MySqlChefSoloSshDriver.class);

    public MySqlChefSoloSshDriver(MySqlNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
        String cookbook = "mysql";
        String fileCachePath = "/tmp/chef-solo";
        String cookbooksPath = "/tmp/chef-solo/cookbooks";
        String workingDir = "~/";
        String solo_rb = workingDir + "solo.rb";
        
        // mysql specific configuration
        String opscodeRepositoryTemplate = "https://github.com/opscode-cookbooks/%s.git";
        String mysqlCookbookPath = String.format(cookbooksPath + "/%s", cookbook);
        String mysqlCookbookJson = String.format(workingDir + "%s.json", cookbook);
        String mysqlCookbookRepository = String.format(opscodeRepositoryTemplate, cookbook);
        
        // configure mysql's dependencies
        String opensslCookbookRepository = String.format(opscodeRepositoryTemplate, "openssl");
        String opensslCookbookPath = String.format(cookbooksPath + "/%s", "openssl");
        String buildEssentialCookbookRepository = String.format(opscodeRepositoryTemplate, "build-essential");
        String buildEssentialCookbookPath = String.format(cookbooksPath + "/%s", "build-essential");
        // configure mysql properties
        String server_root_password = "password";
        String server_repl_password = "password";
        String server_debian_password = "password";
        
        // build installation commands
        List<String> commands = installChefClientAndSolo();
        commands.add(CommonCommands.installPackage("git"));
        commands.add(sudo(String.format("git clone %s %s", buildEssentialCookbookRepository, buildEssentialCookbookPath)));        
        commands.add(sudo(String.format("git clone %s %s", opensslCookbookRepository, opensslCookbookPath)));
        commands.add(sudo(String.format("git clone %s %s", mysqlCookbookRepository, mysqlCookbookPath)));
        
        // configure chef-solo
        commands.add("echo 'file_cache_path \"" + fileCachePath + "\"' >> " + solo_rb + " && " + 
                     "echo 'cookbook_path \""   + cookbooksPath + "\"' >> " + solo_rb + " ; ");
        // configure mysql for chef-solo
        commands.add("echo '" + 
                     "{" + "\"mysql\": { " + 
                     "      \"server_root_password\": \"" + server_root_password + "\"," + 
                     "      \"server_repl_password\": \"" + server_repl_password + "\"," + 
                     "      \"server_debian_password\": \"" + server_debian_password + "\"" + "}, " + 
                     "      \"run_list\":[\"recipe[mysql::server]\"] " + 
                     "}' >> " + mysqlCookbookJson + "; ");
        
        // run chef-solo
        commands.add(sudo(String.format("chef-solo -c %s -j %s; ", solo_rb, mysqlCookbookJson)));
        newScript(INSTALLING).body.append(commands).failOnNonZeroResultCode().execute();
    }

    public static List<String> installChefClientAndSolo() {
        List<String> commands = new LinkedList<String>();
        commands.add(CommonCommands.INSTALL_CURL);
        // NB: chef-solo command is packaged together with chef-client.
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