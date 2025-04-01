/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.e2e;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Network.Ipam;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.function.Consumer;

/*
 * The behaviour of a meshmonitor inside container is not exactly the same as a meshmonitor inside a
 * server. Example: if we stop meshmonitor on a server others will not be able to connect and will get
 * different network errors than when they try to connect to a container that stopped.
 *
 * This code will create container that tests jar version of meshmonitor when run on MacOs and native version when
 * run on other operating systems.
 *
 * For now this should be enough though.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContainerTestBase {

    private static final ImageFromDockerfile IMAGE_WITH_JAR = new ImageFromDockerfile()
            .withFileFromFile("/home/meshmonitor", new File(".").getAbsoluteFile().getParentFile())
            .withFileFromString("/home/meshmonitor/meshmonitor",
                    "#!/bin/bash\n" +
                    "java \\\n" +
                    "     -cp \"/home/meshmonitor/target/meshmonitor-1.0.0-jar-with-dependencies.jar\" \\\n" +
                    "     org.voltdb.meshmonitor.cli.MeshMonitorCommand -i 1 \"$@\"\n"
            )
            .withDockerfileFromBuilder(builder ->
                    builder
                            .from("ghcr.io/graalvm/graalvm-community:21.0.1-ol9-20231024")
                            .copy("/home/meshmonitor", "/home/meshmonitor")
                            .copy("/home/meshmonitor/meshmonitor", "/home/meshmonitor/meshmonitor")
                            .run("chmod +x /home/meshmonitor/meshmonitor")
                            .workDir("/home/meshmonitor/")
                            .build()
            );

    private static final ImageFromDockerfile IMAGE_WITH_NATIVE = new ImageFromDockerfile()
            .withFileFromFile("/home/meshmonitor", new File(".").getAbsoluteFile().getParentFile())
            .withDockerfileFromBuilder(builder ->
                    builder
                            .from("ghcr.io/graalvm/graalvm-community:21.0.1-ol9-20231024")
                            .copy("/home/meshmonitor", "/home/meshmonitor")
                            .copy("/home/meshmonitor/target/meshmonitor", "/home/meshmonitor/meshmonitor")
                            .run("chmod +x /home/meshmonitor/meshmonitor")
                            .workDir("/home/meshmonitor/")
                            .build()
            );

    private static final ImageFromDockerfile IMAGE = SystemUtils.IS_OS_MAC_OSX ? IMAGE_WITH_JAR : IMAGE_WITH_NATIVE;

    public final Network network = Network.builder()
            .createNetworkCmdModifier(modifier -> {
                Ipam.Config config = new Ipam.Config();
                config.withSubnet("192.168.0.0/20");

                Ipam ipam = new Ipam();
                ipam.withConfig(config);

                modifier.withIpam(ipam);
            })
            .build();

    public final StringBuffer logs0 = new StringBuffer();
    public final StringBuffer logs1 = new StringBuffer();
    public final StringBuffer logs2 = new StringBuffer();

    public GenericContainer meshmonitor0 = new GenericContainer(IMAGE)
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> System.out.println(outputFrame.getUtf8StringWithoutLineEnding()))
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> logs0.append(outputFrame.getUtf8String()))
            .waitingFor(Wait.forLogMessage(".*Starting meshmonitor.*", 1))
            .withCommand("/home/meshmonitor/meshmonitor",
                    "-m",
                    "192.168.0.2:12223",
                    "-b",
                    "192.168.0.2:12222"
            )
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd.withIpv4Address("192.168.0.2"))
            .withExposedPorts(12223)
            .withNetworkAliases("m0")
            .withNetwork(network);

    public GenericContainer meshmonitor1 = new GenericContainer(IMAGE)
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> System.out.println(outputFrame.getUtf8StringWithoutLineEnding()))
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> logs1.append(outputFrame.getUtf8String()))
            .waitingFor(Wait.forLogMessage(".*Starting meshmonitor.*", 1))
            .withCommand("/home/meshmonitor/meshmonitor",
                    "-m",
                    "192.168.0.3:12223",
                    "-b",
                    "192.168.0.3:12222",
                    "192.168.0.2"
            )
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd.withIpv4Address("192.168.0.3"))
            .withExposedPorts(12223)
            .withNetworkAliases("m1")
            .withNetwork(network);

    public GenericContainer meshmonitor2 = new GenericContainer(IMAGE)
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> System.out.println(outputFrame.getUtf8StringWithoutLineEnding()))
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> logs2.append(outputFrame.getUtf8String()))
            .waitingFor(Wait.forLogMessage(".*Starting meshmonitor.*", 1))
            .withCommand("/home/meshmonitor/meshmonitor",
                    "-m",
                    "192.168.0.4:12223",
                    "-b",
                    "192.168.0.4:12222",
                    "192.168.0.2"
            )
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd.withIpv4Address("192.168.0.4"))
            .withExposedPorts(12223)
            .withNetworkAliases("m2")
            .withNetwork(network);

    public void clearLogs() {
        logs0.setLength(0);
        logs1.setLength(1);
        logs2.setLength(2);
    }

    @AfterEach
    void tearDown() {
        meshmonitor0.stop();
        meshmonitor1.stop();
        meshmonitor2.stop();

        network.close();
    }
}
