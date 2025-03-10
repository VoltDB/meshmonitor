#
# Copyright (C) 2024-2025 Volt Active Data Inc.
#
# Use of this source code is governed by an MIT
# license that can be found in the LICENSE file or at
# https://opensource.org/licenses/MIT.
#

# syntax=docker/dockerfile:1
FROM --platform=$TARGETPLATFORM  ghcr.io/graalvm/native-image-community:21.0.2-ol9-20240116 AS GRAAL
COPY . /home/meshmonitor
WORKDIR /home/meshmonitor
RUN MAVEN_OPTS=./mvnw -ntp clean package -Pnative

# This is a stage to build a meshmonitor image, previous one is a stage to source files from
FROM --platform=$TARGETPLATFORM gcr.io/distroless/base-debian12

COPY --from=GRAAL \
    /home/meshmonitor/target/ \
    /target/

CMD ["/meshmonitor/meshmonitor"]
