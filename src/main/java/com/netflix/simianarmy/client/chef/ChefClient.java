/*
 *  Copyright 2014 Gutefrage.net GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.simianarmy.client.chef;


import java.util.Iterator;
import java.util.List;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.google.common.net.HostAndPort;
import org.jclouds.chef.domain.Node;
import org.jclouds.ssh.jsch.JschSshClient;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.ssh.SshClient;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.chef.ChefContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jclouds.proxy.internal.GuiceProxyConfig;


/**
 * This client describes the Chef tags as AutoScalingGroup's
 * heavily based on the vsphere client
 *
 * @author florian.pfeiffer@gutefrage.net
 */
public class ChefClient extends AWSClient{

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChefClient.class);

    ChefContext context = null;

    /**
     * Create the specific Client from the given strategy and connection.
     */
    public ChefClient(ChefContext context) {
        super("region-");
        this.context = context;
    }


    @Override
    public List<AutoScalingGroup> describeAutoScalingGroups(String... names) {

        final ChefGroups groups = new ChefGroups();
        ObjectMapper mapper = new ObjectMapper();


        //Get all nodes from Chef, and put hosts depending on their tags into the different groups.
        //This means, that a node can end up in more than one group
        for(Node node: context.getChefService().listNodes()) {

            if(node.getNormal().get("tags")!=null) {

                try {
                    JsonNode jnode = mapper.readTree(node.getNormal().get("tags").toString());

                    Iterator<JsonNode> tags = jnode.iterator();
                    while (tags.hasNext()) {
                        groups.addInstance(node.getName(), tags.next().asText());
                    }
                }
                catch(Exception e)
                {
                    LOGGER.error(String.format("Can't parse tag json for node %s", node.getName()));
                }

            }
        }

        return groups.asList();
    }

    @Override
    /**
     * currently we don't support this with chef :(
     */
    public void terminateInstance(String instanceId) {
        LOGGER.info(String.format("We want to kill %s but it's not supported with ChefClient.", instanceId));
    }



    @Override
    /**
     * connect to the given instance with the given credentials
     */
    public SshClient connectSsh(String instanceId, LoginCredentials credentials) {

        for(Node node: context.getChefService().listNodes()) {

            if(node.getName().equals(instanceId)) {
                HostAndPort socket = HostAndPort.fromString(node.getName()).withDefaultPort(22);

                //Use JschSshClient directly, had some problems when trying to go through jclouds
                SshClient ssh = new JschSshClient(new GuiceProxyConfig(), BackoffLimitedRetryHandler.INSTANCE, socket, credentials, 10);
                LOGGER.info(String.format("Opening ssh connection to %s (%s)", instanceId, socket.toString()));
                ssh.connect();
                return ssh;
            }
        }

        return null;
    }

}

