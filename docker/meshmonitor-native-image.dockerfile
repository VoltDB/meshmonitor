# syntax=docker/dockerfile:1
FROM --platform=$TARGETPLATFORM  ghcr.io/graalvm/native-image-community:21.0.2-ol9-20240116 AS GRAAL
COPY . /home/meshmonitor
WORKDIR /home/meshmonitor
RUN MAVEN_OPTS=--enable-preview ./mvnw -ntp clean package -P native

# This is a stage to build a meshmonitor image, previous one is a stage to source files from
FROM --platform=$TARGETPLATFORM gcr.io/distroless/base-debian12

COPY --from=GRAAL \
    /home/meshmonitor/target/meshmonitor \
    /home/meshmonitor/target/meshmonitor_completion.sh \
    /home/meshmonitor/target/meshmonitor-1.0.0-jar-with-dependencies.jar \
    /home/meshmonitor/target/generated-docs/meshmonitor.1 \
    /home/meshmonitor/target/generated-docs/meshmonitor.html \
    /meshmonitor/

CMD ["/meshmonitor/meshmonitor"]
