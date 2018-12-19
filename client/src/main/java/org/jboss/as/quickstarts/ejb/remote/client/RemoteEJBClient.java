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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

public class RemoteEJBClient {

    public static void main(String[] args) throws Exception {
        List<RemoteCalculator> remoteCalculators = new CopyOnWriteArrayList<>();
        List<InitialContext> initialContexts = new CopyOnWriteArrayList<>();
        IntStream.range(0, 100).parallel().forEach(i -> {
            RemoteCalculator remoteCalculator = null;
            try {
                InitialContext initialContext = createInitialContext();
                initialContexts.add(initialContext);
                remoteCalculator = lookupRemoteStatelessCalculator(initialContext);
                System.out.println("Created remote calculator #" + i);
                int add = remoteCalculator.add(1, 1);
                remoteCalculators.add(remoteCalculator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        while(true) {
            System.out.println("Idling...");
            Thread.sleep(5000);
        }
    }

    private static void invokeStatelessBean() throws NamingException {
        // Let's lookup the remote stateless calculator
        final RemoteCalculator statelessRemoteCalculator = lookupRemoteStatelessCalculator(createInitialContext());
        System.out.println("Obtained a remote stateless calculator for invocation");
        // invoke on the remote calculator
        int a = 204;
        int b = 340;
        System.out.println("Adding " + a + " and " + b + " via the remote stateless calculator deployed on the server");
        int sum = statelessRemoteCalculator.add(a, b);
        System.out.println("Remote calculator returned sum = " + sum);
        if (sum != a + b) {
            throw new RuntimeException("Remote stateless calculator returned an incorrect sum " + sum + " ,expected sum was "
                    + (a + b));
        }
        // try one more invocation, this time for subtraction
        int num1 = 3434;
        int num2 = 2332;
        System.out.println("Subtracting " + num2 + " from " + num1
                + " via the remote stateless calculator deployed on the server");
        int difference = statelessRemoteCalculator.subtract(num1, num2);
        System.out.println("Remote calculator returned difference = " + difference);
        if (difference != num1 - num2) {
            throw new RuntimeException("Remote stateless calculator returned an incorrect difference " + difference
                    + " ,expected difference was " + (num1 - num2));
        }
    }

    private static RemoteCalculator lookupRemoteStatelessCalculator(InitialContext initialContext) throws NamingException {
        InitialContext ctx = initialContext;
        return (RemoteCalculator) ctx.lookup("ejb-remote-server-side-1.0.0-SNAPSHOT/CalculatorBean!org.jboss.as.quickstarts.ejb.remote.stateless.RemoteCalculator");
    }

    private static InitialContext createInitialContext() throws NamingException {
        Properties properties = new Properties();
        properties.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
        properties.put("java.naming.provider.url", "http-remoting://jboss-app-remote-ejb.apps.ocppoc.balgroupit.com:80");

        properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        properties.put("jboss.naming.client.ejb.context", "true");
        return new InitialContext(properties);
    }

}
