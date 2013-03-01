package io.brooklyn.puppet.entities;

import static brooklyn.entity.basic.lifecycle.CommonCommands.*;
import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNodeImpl;
import brooklyn.location.basic.SshMachineLocation;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */

public class MySqlPuppetSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

    public static final Logger log = LoggerFactory.getLogger(MySqlPuppetSshDriver.class);

    public MySqlPuppetSshDriver(MySqlNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
        String puppetModules = "/etc/puppet/modules";
        List<String> commands = Lists.newArrayList();
        commands.add(installPuppet());
        commands.add(sudo("puppet module install puppetlabs/mysql"));
        commands.add(sudo(format("puppet apply %s/mysql/tests/server.pp --verbose", puppetModules)));
        newScript(INSTALLING).body.append(commands).failOnNonZeroResultCode().execute();
    }

    /**
     * Returns the commands that installs Puppet.
     * 
     * @return the commands that installs Puppet.
     */
    public static String installPuppet() {
        String failure = format("(echo \"WARNING: no known/successful way found to install Puppet\")");
        List<String> commands = new LinkedList<String>();
        commands.add(installPuppetOnDebianAndUbuntu());
        commands.add(installPuppetOnRedHatAndDerivatives());
        return alternatives(commands, failure);
    }

    private static String installPuppetOnRedHatAndDerivatives() {
        return exists("yum",
                "unset OS_MAJOR; OS_MAJOR=`head -1 /etc/issue | awk '{ print $3 }' | cut -d'.' -f1`",
                sudo("rpm -ivh http://yum.puppetlabs.com/el/$OS_MAJOR/products/i386/puppetlabs-release-$OS_MAJOR-6.noarch.rpm"),
                sudo("yum clean all"), sudo("yum -y --nogpgcheck install puppet puppet-server"));
    }

    private static String installPuppetOnDebianAndUbuntu() {
        return exists("apt-get", "unset CODENAME; CODENAME=`lsb_release -c -s`", INSTALL_WGET,
                "wget http://apt.puppetlabs.com/puppetlabs-release-$CODENAME.deb",
                sudo("dpkg -i ./puppetlabs-release-$CODENAME.deb"), sudo("apt-get update"),
                sudo("apt-get -f -y -qq --force-yes install puppet puppetmaster"));
    }

    @Override
    public void customize() {
        log.debug("Nothing to customize");
    }

    @Override
    public void launch() {
        List<String> commands = Lists.newArrayList();
        commands.add("pidof mysqld > pid.txt");
        newScript(LAUNCHING).body.append(commands).updateTaskAndFailOnNonZeroResultCode().execute();
    }

    @Override
    public boolean isRunning() {
        return newScript(ImmutableMap.of("usePidFile", true), CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(ImmutableMap.of("usePidFile", true), STOPPING).execute();
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