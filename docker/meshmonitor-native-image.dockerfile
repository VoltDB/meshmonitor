# This file is part of VoltDB.
# Copyright (C) 2023 Volt Active Data Inc.

# syntax=docker/dockerfile:1
FROM --platform=$TARGETPLATFORM  ghcr.io/graalvm/native-image-community:21.0.1-ol9-20231024 AS GRAAL
COPY ../meshmonitor /home/meshmonitor
WORKDIR /home/meshmonitor
RUN ./mvnw clean package -P native

# This is a stage to build a meshmonitor image, previous one is a stage to source files from
FROM --platform=$TARGETPLATFORM gcr.io/distroless/base-debian12

COPY --from=GRAAL /home/meshmonitor/target/meshmonitor /meshmonitor/meshmonitor
CMD ["/meshmonitor/meshmonitor"]
