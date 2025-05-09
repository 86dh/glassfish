/*
 * Copyright (c) 2022, 2025 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.main.admin.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.main.itest.tools.GlassFishTestEnvironment;
import org.glassfish.main.itest.tools.asadmin.Asadmin;
import org.glassfish.main.itest.tools.asadmin.AsadminResult;
import org.glassfish.tests.utils.ServerUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.glassfish.main.admin.test.ConnectionUtils.getURL;
import static org.glassfish.main.itest.tools.asadmin.AsadminResultMatcher.asadminOK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Tom Mueller
 */
@TestMethodOrder(OrderAnnotation.class)
public class ClusterITest {

    private static final Queue<Integer> RANDOM_FREE_PORTS = ServerUtils.getFreePorts(16);
    private static final String TEST_APP_NAME = "testapp";
    private static final int PORT_INSTANCE_1 = RANDOM_FREE_PORTS.remove();
    private static final int PORT_INSTANCE_2 = RANDOM_FREE_PORTS.remove();
    private static final String CLUSTER_NAME = "eec1";
    private static final String INSTANCE_NAME_1 = "eein1-with-a-very-very-very-long-name";
    private static final String INSTANCE_NAME_2 = "eein2";
    private static final String URL_INSTANCE_1 = "http://localhost:" + PORT_INSTANCE_1;
    private static final String URL_INSTANCE_2 = "http://localhost:" + PORT_INSTANCE_2;
    private static final Asadmin ASADMIN = GlassFishTestEnvironment.getAsadmin(false);
    private static final AtomicBoolean INSTANCES_REACHABLE = new AtomicBoolean();
    private static final AtomicBoolean APP_DEPLOYED = new AtomicBoolean();

    @AfterAll
    public static void cleanup() {
        ASADMIN.exec("stop-local-instance", "--kill", INSTANCE_NAME_1);
        ASADMIN.exec("stop-local-instance", "--kill", INSTANCE_NAME_2);
    }

    @Test
    @Order(1)
    public void createClusterTest() {
        assertThat(ASADMIN.exec("create-cluster", CLUSTER_NAME), asadminOK());
    }

    @Test
    @Order(2)
    public void deployAppToClusterTest() {
        String warFile = TestResources.createSimpleWarDeployment(TEST_APP_NAME).getAbsolutePath();
        assertThat(ASADMIN.exec("deploy", "--target", CLUSTER_NAME, "--name", TEST_APP_NAME, "--contextroot", TEST_APP_NAME,
                warFile), asadminOK());
    }

    @Test
    @Order(3)
    public void createInstancesTest() {
        // Create a cluster with two instances
        assertThat(
                ASADMIN.exec("create-local-instance", "--cluster", CLUSTER_NAME, "--systemproperties",
                        "HTTP_LISTENER_PORT=" + PORT_INSTANCE_1
                        + ":HTTP_SSL_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_SSL_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":JMX_SYSTEM_CONNECTOR_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_SSL_MUTUALAUTH_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":JMS_PROVIDER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":ASADMIN_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove(),
                        INSTANCE_NAME_1), asadminOK());

        assertThat(
                ASADMIN.exec("create-local-instance", "--cluster", CLUSTER_NAME, "--systemproperties",
                        "HTTP_LISTENER_PORT=" + PORT_INSTANCE_2
                        + ":HTTP_SSL_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_SSL_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":JMX_SYSTEM_CONNECTOR_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":IIOP_SSL_MUTUALAUTH_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":JMS_PROVIDER_PORT=" + RANDOM_FREE_PORTS.remove()
                        + ":ASADMIN_LISTENER_PORT=" + RANDOM_FREE_PORTS.remove(),
                        INSTANCE_NAME_2), asadminOK());
    }

    @Test
    @Order(4)
    public void startInstancesTest() {
        assertThat(ASADMIN.exec(60_000, "start-local-instance", INSTANCE_NAME_1), asadminOK());
        assertThat(ASADMIN.exec(60_000, "start-local-instance", INSTANCE_NAME_2), asadminOK());
    }

    @Test
    @Order(5)
    public void checkClusterTest() {
        assertThat(ASADMIN.exec("list-instances"), asadminOK());
        assertThat(getURL(URL_INSTANCE_1), stringContainsInOrder("GlassFish Server"));
        assertThat(getURL(URL_INSTANCE_2), stringContainsInOrder("GlassFish Server"));
        INSTANCES_REACHABLE.set(true);
    }

    @Test
    @Order(6)
    public void checkDeploymentTest() {
        final AsadminResult result = ASADMIN.exec("list-applications", CLUSTER_NAME);
        assertThat(result, asadminOK());
        assertThat("list-applications output", result.getStdOut(), containsString(TEST_APP_NAME));
        APP_DEPLOYED.set(true);
        assertThat(getURL(URL_INSTANCE_1 + "/" + TEST_APP_NAME), stringContainsInOrder("Simple test app"));
        assertThat(getURL(URL_INSTANCE_2 + "/" + TEST_APP_NAME), stringContainsInOrder("Simple test app"));
    }

    @Test
    @Order(10)
    public void retrieveCollectedLogFilesTest() throws IOException {
        assumeTrue(INSTANCES_REACHABLE.get());

        Path logDir = Files.createTempDirectory("log");

        AsadminResult result = ASADMIN.exec(
                "collect-log-files",
                "--target", CLUSTER_NAME,
                "--retrieve", logDir.toAbsolutePath().toString());
        assertThat(result, asadminOK());

        String path = StringUtils.substringBetween(result.getStdOut(), "Created Zip file under ", ".\n");
        assertNotNull(path, () -> "zip file path parsed from " + result.getStdOut());

        Path logFile = Path.of(path);
        assertAll(
                () -> assertThat(Files.size(logFile), greaterThan(2_000L)),
                () -> assertThat(logFile.getFileName().toString(), allOf(startsWith("log"), endsWith(".zip")))
        );

        Files.deleteIfExists(logFile);
        Files.deleteIfExists(logDir);
    }

    @Test
    @Order(11)
    public void collectLogFilesFromInstanceTest() {
        assumeTrue(INSTANCES_REACHABLE.get());

        AsadminResult result = ASADMIN.exec("collect-log-files", "--target", INSTANCE_NAME_1);
        assertAll(
                () -> assertThat(result, not(asadminOK())),
                () -> assertThat(result.getStdOut(), containsString("Command collect-log-files failed")),
                () -> assertThat(result.getStdErr(), containsString(
                        "The collect-log-files command is not allowed on target " + INSTANCE_NAME_1
                        + " because it is part of cluster " + CLUSTER_NAME))
        );
    }

    @Test
    @Order(20)
    public void undeployAppsTest() {
        assumeTrue(APP_DEPLOYED.get());
        assertThat(ASADMIN.exec("undeploy", "--target", CLUSTER_NAME, TEST_APP_NAME), asadminOK());
    }

    @Test
    @Order(21)
    public void stopInstancesTest() {
        assertThat(ASADMIN.exec("stop-local-instance", "--kill", INSTANCE_NAME_1), asadminOK());
        assertThat(ASADMIN.exec("stop-local-instance", "--kill", INSTANCE_NAME_2), asadminOK());
    }

    @Test
    @Order(22)
    public void deleteInstancesTest() {
        assertThat(ASADMIN.exec("delete-local-instance", INSTANCE_NAME_1), asadminOK());
        assertThat(ASADMIN.exec("delete-local-instance", INSTANCE_NAME_2), asadminOK());
    }

    @Test
    @Order(23)
    public void deleteClusterTest() {
        assertThat(ASADMIN.exec("delete-cluster", CLUSTER_NAME), asadminOK());
    }

}
