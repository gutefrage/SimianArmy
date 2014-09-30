package com.netflix.simianarmy.client.chef;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicChaosMonkeyContext;
import org.jclouds.ContextBuilder;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;

/**
 * This provides a Client for Chef
 * @author florian.pfeiffer@gutefrage.net
 */
public class ChefContext  extends BasicChaosMonkeyContext {
    @Override
    protected void createClient() {
        MonkeyConfiguration config = configuration();
        String privateKey = null;
        String chefURL = config.getStrOrElse("simianarmy.client.chef.url", "http://127.0.0.1:4242");
        String chefClient = config.getStrOrElse("simianarmy.client.chef.clientname", "root");


        //copied code from from SshConfig
        String chefKeyPath = config.getStrOrElse("simianarmy.client.chef.key", null);
        if (chefKeyPath != null) {
            chefKeyPath = chefKeyPath.trim();
            if (chefKeyPath.startsWith("~/")) {
                String home = System.getProperty("user.home");
                if (!Strings.isNullOrEmpty(home)) {
                    if (!home.endsWith("/")) {
                        home += "/";
                    }
                    chefKeyPath = home + chefKeyPath.substring(2);
                }
            }

            try {
                privateKey = Files.toString(new File(chefKeyPath), Charsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the specified Chef key: " + chefKeyPath, e);
            }
        }

        org.jclouds.chef.ChefContext context = ContextBuilder.newBuilder("chef")
                .endpoint(chefURL)
                .credentials(chefClient, privateKey)
                .buildView(org.jclouds.chef.ChefContext.class);


        final ChefClient client = new ChefClient(context);
        setCloudClient(client);
    }
}

