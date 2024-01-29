/* This file is part of VoltDB.
 * Copyright (C) 2023 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb.meshmonitor.kubernetes;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.voltdb.meshmonitor.kubernetes.MeshmonitorPod.fromV1Pod;

public class PodWatcher implements AutoCloseable {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setUncaughtExceptionHandler((t, e) -> System.err.printf("[%s] %s", t.getName(), e))
                    .build()
    );

    private final String namespace;
    private final String labelSelector;

    private volatile List<MeshmonitorPod> podIps;

    PodWatcher(String namespace, String labelSelector) {
        this.namespace = namespace;
        this.labelSelector = labelSelector;
    }

    public void start() {
        System.out.println("PodWatcher.start");
        scheduler.scheduleAtFixedRate(this::updatePodList, 5, 15, TimeUnit.SECONDS);
    }

    void updatePodList() {
        System.out.println("PodWatcher.updatePodList");
        try {
            CoreV1Api api = new CoreV1Api();
            V1PodList v1PodList = api.listNamespacedPod(
                    namespace,
                    "false",
                    false,
                    null,
                    null,
                    labelSelector,
                    100,
                    null,
                    null,
                    false,
                    null,
                    false);
            System.out.println("v1PodList = " + v1PodList);

            podIps = v1PodList.getItems()
                    .stream()
                    .flatMap((V1Pod v1Pod) -> fromV1Pod(v1Pod).stream())
                    .collect(toList());

            System.out.println("podIps = " + podIps);
        } catch (ApiException e) {
            System.out.println("Exception (" + e.getCode() + ")\n" + e.getResponseBody());
        }
    }

    List<MeshmonitorPod> getPodIps() {
        return podIps;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
