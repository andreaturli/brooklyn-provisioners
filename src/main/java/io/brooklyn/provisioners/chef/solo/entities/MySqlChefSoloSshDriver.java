package io.brooklyn.provisioners.chef.solo.entities;

import static brooklyn.util.ssh.CommonCommands.sudo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.database.mysql.MySqlDriver;
import brooklyn.entity.database.mysql.MySqlNodeImpl;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.ssh.CommonCommands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * The SSH implementation of the {@link MySqlDriver}.
 */
public class MySqlChefSoloSshDriver extends AbstractSoftwareProcessSshDriver implements MySqlDriver {

    public static final Logger log = LoggerFactory.getLogger(MySqlChefSoloSshDriver.class);
        
    private String cookbook = "mysql";
    
    public String getPassword() { return getEntity().getPassword(); }
    
    public MySqlChefSoloSshDriver(MySqlNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
        String opscodeRepositoryTemplate = "https://github.com/opscode-cookbooks/%s.git";
        String buildEssentialCookbookRepository = String.format(opscodeRepositoryTemplate, "build-essential");
        String opensslCookbookRepository = String.format(opscodeRepositoryTemplate, "openssl");
        String mysqlCookbookRepository = String.format(opscodeRepositoryTemplate, "mysql");
        
        List<String> commands = installChefClientAndChefSolo();
        commands.addAll(installCookbookAndItsDependencies(ImmutableMap.of(
                                    "build-essential", createURI(buildEssentialCookbookRepository),
                                    "openssl", createURI(opensslCookbookRepository),
                                    "mysql", createURI(mysqlCookbookRepository))));
        commands.add(createRunList(cookbook));
        newScript(INSTALLING).body.append(commands).failOnNonZeroResultCode().execute();
    }

    private URI createURI(String repository) {
        try {
            return new URI(repository);
        } catch (URISyntaxException e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void customize() {
        log.debug("Nothing to customize");
    }
    
    @Override
    public void launch() {
        log.info(String.format("Starting entity %s at %s", this, getLocation()));
        List<String> commands = ImmutableList.of(runChefSolo(cookbook));
        newScript(LAUNCHING).body.append(commands).execute();
    }
    
    @Override
    public boolean isRunning() {
        List<String> commands = ImmutableList.of("ps aux | grep [m]ysql");
        return newScript(CHECK_RUNNING).body.append(commands).execute() == 0;
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

    ///////// move out to CommonCommands //////////////////
    private static final String DEFAULT_FILE_CACHE_PATH = "/tmp/chef-solo";
    private static final String DEFAULT_COOKBOOKS_PATH = DEFAULT_FILE_CACHE_PATH + "/cookbooks/";
    private static final String DEFAULT_SOLO_RB = DEFAULT_FILE_CACHE_PATH + "/solo.rb";
    private String runChefSolo(String cookbook) {
        return sudo(String.format("chef-solo -c %s -j %s/%s.json", DEFAULT_SOLO_RB, getInstallDir(), cookbook));
    }

    private String createRunList(String cookbook) {
        return String.format("echo '" + 
                "{" + 
                "\"mysql\": { " + 
                "      \"server_root_password\" : \"%s\", " + 
                "      \"server_repl_password\" : \"%s\", " + 
                "      \"server_debian_password\" : \"%s\" " + 
                "}, " + 
                "      \"run_list\":[\"recipe[mysql::server]\"] " + 
                "}' >> %s.json ; ", getPassword(), getPassword(), getPassword(), cookbook);
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
        // configure chef-solo
        commands.add(String.format("mkdir -p %s ", DEFAULT_FILE_CACHE_PATH));
        commands.add(String.format("echo 'file_cache_path \"%s\"' >> %s ", DEFAULT_FILE_CACHE_PATH, DEFAULT_SOLO_RB));
        commands.add(String.format("echo 'cookbook_path \"%s\"' >> %s ", DEFAULT_COOKBOOKS_PATH, DEFAULT_SOLO_RB));
        return commands;
    }
    
}