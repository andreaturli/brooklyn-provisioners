package io.brooklyn.chef.solo.entities;

import static brooklyn.entity.basic.lifecycle.CommonCommands.sudo;
import static java.lang.String.format;

import java.util.List;

import org.jclouds.scriptbuilder.statements.chef.InstallChefGems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.location.basic.SshMachineLocation;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */

public class MySqlChefSoloSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

   public static final Logger log = LoggerFactory.getLogger(MySqlChefSoloSshDriver.class);

   public MySqlChefSoloSshDriver(MySqlNode entity, SshMachineLocation machine) {
      super(entity, machine);
   }

   /**
    * as chef-solo sets cookbook paths to /tmp/chef-solo/cookbooks, 
    * we need to git clone mysql to /tmp/chef-solo/cookbooks
    */
    @Override
    public void install() {
        String cookbook = "mysql";
        String cookbookPath = String.format("/tmp/chef-solo/cookbooks/%s", cookbook);
        String opscodeRepoTemplate = "https://github.com/opscode-cookbooks/%s.git";
        String cookbookJson = String.format("%s.json", cookbook);
        String cookbookRepo = String.format(opscodeRepoTemplate, cookbook);
        String server_root_password = "password";
        String server_repl_password = "password";
        String server_debian_password = "password";

        List<String> commands = CommonCommands.installChefSolo();
        commands.add(CommonCommands.installPackage("git"));
        
        commands.add(sudo(String.format("git clone %s %s", cookbookRepo, cookbookPath)));
        // clone its dependencies
        String opensslRepo = String.format(opscodeRepoTemplate, "openssl");
        String cookbookDepPath = String.format("/tmp/chef-solo/cookbooks/%s", "openssl");
        commands.add(sudo(String.format("git clone %s %s", opensslRepo, cookbookDepPath)));
        
        commands.add("echo '" +
        		"{" + "\"mysql\": { " 
                + "      \"server_root_password\": \"" + server_root_password + "\","
                + "      \"server_repl_password\": \"" + server_repl_password + "\","
                + "      \"server_debian_password\": \"" + server_debian_password + "\"" + 
                "}, "
                + "    \"run_list\":[\"recipe[mysql::server]\"] " + "  }' >> ~/" + cookbookJson + "; ");

        commands.add(sudo(String.format("chef-solo " + "-j ~/%s; ", cookbookJson)));

        newScript(INSTALLING).body.append(commands).failOnNonZeroResultCode().execute();
    }

   @Override
   public void customize() {
      log.debug("Nothing to customize");
   }

   @Override
   public void launch() {
      log.info(String.format("Starting entity %s at %s", this, getLocation()));
      // newScript(LAUNCHING).body.append(callPgctl("start",
      // false)).failOnNonZeroResultCode().execute();
   }

   @Override
   public boolean isRunning() {
      return true;
      // return newScript(CHECK_RUNNING).body.append(callPgctl("status",
      // false)).execute() == 0;
   }

   @Override
   public void stop() {
      // newScript(STOPPING).body.append(callPgctl("stop",
      // false)).failOnNonZeroResultCode().execute();
   }

   @Override
   public MySqlNode getEntity() {
      return (MySqlNode) super.getEntity();
   }
}