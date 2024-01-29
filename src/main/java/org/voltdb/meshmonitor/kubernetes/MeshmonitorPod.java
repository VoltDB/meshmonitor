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

import io.kubernetes.client.openapi.models.V1Pod;

import java.util.Optional;

record MeshmonitorPod(String podIp, String hostIp, String phase) {

    public static Optional<MeshmonitorPod> fromV1Pod(V1Pod v1Pod) {
        return Optional.ofNullable(v1Pod.getStatus())
                .map(status -> new MeshmonitorPod(
                        status.getPodIP(),
                        status.getHostIP(),
                        status.getPhase()));
    }
}
