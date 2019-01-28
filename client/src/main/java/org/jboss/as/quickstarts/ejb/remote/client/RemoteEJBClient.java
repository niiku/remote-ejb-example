/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejb.remote.client;

import org.jboss.as.quickstarts.ejb.remote.stateless.RemoteCalculator;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

public class RemoteEJBClient {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Please provide the following arguments:");
            System.out.println("--host # Declaring the Remote Server e.g. localhost:8080");
            System.out.println("--connections # The amount of connections to create e.g. 200");
            System.exit(1);
        }
        Integer connections = 0;
        String host = "";
        for (int i = 0; i < args.length; i++) {
            if ("--host".equals(args[i])) {
                host = args[i + 1];
            }
            if ("--connections".equals(args[i])) {
                connections = Integer.valueOf(args[i + 1]);
            }
        }
        if ("".equals(host) || connections == 0) {
            throw new IllegalArgumentException("Invalid host or connection amount (host: " + host + "; connections: " + connections + ")");
        }

//        Properties p = new Properties();
//        p.put("connect.options.org.xnio.Options.READ_TIMEOUT", 60000);
//        p.put("connect.options.org.xnio.Options.KEEP_ALIVE", true);
//        p.put("org.jboss.remoting3.RemotingOptions.HEARTBEAT_INTERVAL", 30000);
//        EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(p);
//        ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc);
//        EJBClientContext.setSelector(selector);

        final String finalHost = host;

        List<RemoteCalculator> remoteCalculators = new CopyOnWriteArrayList<>();
        List<InitialContext> initialContexts = new CopyOnWriteArrayList<>();
        IntStream.range(0, connections).parallel().forEach(i -> {
            RemoteCalculator remoteCalculator = null;
            try {
                InitialContext initialContext = createInitialContext(finalHost);
                initialContexts.add(initialContext);
                remoteCalculator = lookupRemoteStatelessCalculator(initialContext);
                System.out.println("Created remote calculator #" + i);
                int add = remoteCalculator.add(1, 1);
                remoteCalculators.add(remoteCalculator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        while (true) {
            System.out.println("Idling...");
            Thread.sleep(5000);
        }
    }

    private static RemoteCalculator lookupRemoteStatelessCalculator(InitialContext initialContext) throws NamingException {
        InitialContext ctx = initialContext;
        return (RemoteCalculator) ctx.lookup("ejb-remote-server-side-1.0.0-SNAPSHOT/CalculatorBean!org.jboss.as.quickstarts.ejb.remote.stateless.RemoteCalculator");
    }

    private static InitialContext createInitialContext(String remoteHost) throws NamingException {
        Properties properties = new Properties();
        properties.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
        properties.put("java.naming.provider.url", "http-remoting://" + remoteHost);
        properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        properties.put("jboss.naming.client.ejb.context", "true");


        return new InitialContext(properties);
    }

}
