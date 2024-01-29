# This file is part of VoltDB.
# Copyright (C) 2023 Volt Active Data Inc.

# syntax=docker/dockerfile:1
FROM --platform=$TARGETPLATFORM ghcr.io/graalvm/graalvm-community:21.0.1-ol9-20231024 AS GRAAL
COPY ../meshmonitor /home/meshmonitor
WORKDIR /home/meshmonitor
RUN ./mvnw clean package

# This is a stage to build a meshmonitor image, previous one is a stage to source files from
FROM --platform=$TARGETPLATFORM ghcr.io/graalvm/graalvm-community:21.0.1-ol9-20231024

COPY --from=GRAAL /home/meshmonitor/target /meshmonitor/
CMD ["java", "-jar", "/meshmonitor/meshmonitor-1.0.0-jar-with-dependencies.jar"]
