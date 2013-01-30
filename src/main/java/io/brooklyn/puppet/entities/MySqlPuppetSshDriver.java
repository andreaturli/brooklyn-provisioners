package io.brooklyn.puppet.entities;

import static brooklyn.entity.basic.lifecycle.CommonCommands.sudo;
import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.location.basic.SshMachineLocation;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */

public class MySqlPuppetSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

   public static final Logger log = LoggerFactory.getLogger(MySqlPuppetSshDriver.class);

   public MySqlPuppetSshDriver(MySqlNode entity, SshMachineLocation machine) {
      super(entity, machine);
   }

   @Override
   public void install() {
      String puppetModules = "/etc/puppet/modules";
      List<String> commands = Lists.newArrayList();//installPuppet();
      commands.add(sudo("puppet module install puppetlabs/mysql"));
      commands.add(sudo(format("puppet apply %s/mysql/tests/server.pp --verbose", puppetModules)));
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