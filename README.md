[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/VoltDB/meshmonitor/blob/master/LICENSE)
[![Tests](https://github.com/VoltDB/meshmonitor/actions/workflows/test.yml/badge.svg)](https://github.com/VoltDB/meshmonitor/actions/workflows/test.yml)
[![GitHub Release](https://img.shields.io/github/v/release/VoltDB/meshmonitor)](https://github.com/VoltDB/meshmonitor/releases)

# Table of Contents

1. [Overview](#overview)
    1. [What Meshmonitor Can (and Can't) Tell You](#what-meshmonitor-can-and-cant-tell-you)
1. [What Meshmonitor does](#what-meshmonitor-does)
    1. [Output](#output)
    1. [Interpreting Results](#interpreting-results)
1. [Obtaining Meshmonitor](#obtaining-meshmonitor)
1. [Using Meshmonitor](#using-meshmonitor)
    1. [Openmetrics / Prometheus](#openmetrics--prometheus)
       1. [List of metrics](#list-of-metrics)
    1. [Datadog monitoring](#datadog-monitoring)
    1. [Running from a jar](#running-from-a-jar)

# Overview

Meshmonitor is a tool for monitoring network issues such as network delays and instability, mysterious timeouts, hangs,
and scheduling problems that delay message passing.

While it can be used as a general network monitoring tool it simulates the Volt heartbeat paths and is intended to
diagnose situations when sites are experiencing dead host timeouts without any obvious network event.

Meshmonitor can be run alongside Volt. It has very low overhead on the CPUs and network. It can also be helpful to run
without Volt - as a proof point that your environment is going to have adequate baseline stability for running Volt.

## What Meshmonitor Can (and Can't) Tell You

First - an analogy. Think of Volt as a fast car. Like all cars, the quality of the streets impacts the top speed at
which you can travel. If the streets you drive on are poorly paved, have random lane closings, or a lot of traffic
lights then your fast car goes no faster than any old jalopy.

Environmental blips in scheduling, networking, CPU accesses are like potholes to Volt. If a Volt site thread can't
run for 50ms - then your application can experience long-tail latency problems. If the heart beating between cluster
nodes is delayed for long enough, then some nodes may get ejected from the cluster.

The following is an ever-growing list of things that the Volt support team has seen when looking at customers' Mesh
Monitor data:

1. Batch network copies/backups that are doing a high IO load that linux decides is more important than scheduling
   Volt (a solution is to throttle these jobs)
2. Other processes on the system taking too much CPU
3. VMs/Containers that are starved by neighbors
4. VMs/Containers with incorrect/inadequate thread pinning
5. VM/Containers that are migrating
6. Power save modes that slow down "idle" processors
7. Boot/grub kernel setting of idle=poll
8. Network congestion between particular nodes/pods

Causes of latency/timeouts that are not visible using meshmonitor (but may be visible in volt.log):

1. GC stalls
2. Volt memory compaction delays (for versions prior to V12.0)

# What Meshmonitor does

Each meshmonitor process has 2 threads: *send* and *receive* for every node it is connected to. These threads measure
and report 3 metrics:

1. **Receiving heartbeats.** A *receive* thread that is blocked reading the socket that receives messages sent from the
   other servers. This is the main metric to look at. It should be close to the heartbeat interval (set using `-p`
   option with default value of 5ms).
2. **Scheduling jitter.** A *send* thread wakes up every 5 milliseconds and sends heartbeats to all the other servers
   running meshmonitor. It reports time between wakeups which should be close to 5ms. This tracks the liveness of the
   server (i.e. ability of a thread to get scheduled in a timely manner and send a message out.)
3. **Timestamp differences**.The *receive* thread also measures the difference in time between the timestamp encoded in
   the heartbeat by *send* thread and when the heartbeat was processed by *receive* thread.

## Output

All messages printed by Meshmonitor contain event time (`HH:mm:ss`) and an IP address of the node that the message
pertains to. If a message has no such context then the IP column will be empty:

```console
09:08:51 [   172.31.10.72] New remote endpoint - establishing connection
09:08:51 [   172.31.10.72] Connecting
09:08:51 [   172.31.10.72] Connected
09:08:51 [   172.31.10.72] Handshake sent
09:08:51 [    172.31.14.3] New remote endpoint - establishing connection
09:08:51 [    172.31.14.3] Connecting
09:08:51 [    172.31.14.3] Connected
09:08:51 [    172.31.14.3] Handshake sent
09:08:51 [   172.31.9.146] Received connection
09:08:51 [   172.31.9.146] New remote endpoint - establishing connection
09:08:51 [   172.31.9.146] Connecting
09:08:51 [   172.31.9.146] Connected
09:08:51 [   172.31.9.146] Handshake sent
09:08:52 [    172.31.14.3] Broken pipe
```

There are 3 kinds of measurements:

* ping (delta receive) - delta between receiving heartbeats
* jitter (delta send) - delta between sending heartbeats
* timestamp delta - delta between remotely recorded sent timestamp and locally recorded receive time

Meshmonitor will print histograms of each of the three tracked values. All of these values need to be interpreted with
the `--ping` interval in mind (default 5ms) that is included in the measurement values. The values that are printed are
max, mean, and percentiles: 99th, 99.9th, and 99.99th:

```console
[               ] --------receive-(ms)--------- ---------delta-(ms)---------- ---------send-(ms)-----------
[               ]   Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99
[   172.31.10.72]   5.2   5.1   5.1   5.2   5.2|  0.2   0.0   0.0   0.1   0.2|  5.1   5.1   5.1   5.1   5.1
[    172.31.14.3]   5.3   5.1   5.1   5.1   5.3|  0.4   0.2   0.2   0.2   0.4|  5.8   5.1   5.1   5.5   5.8
[   172.31.9.146]   5.1   5.1   5.1   5.1   5.1|  5.1   2.6   5.0   5.1   5.1|  5.1   5.1   5.1   5.1   5.1
[   172.31.5.177]   5.1   5.1   5.1   5.1   5.1|  5.2   2.8   5.2   5.2   5.2|  5.1   5.1   5.1   5.1   5.1
```

Measurements exceeding `--threshold` (default 20ms) will be printed in yellow. Those that exceed 1 second will be printed in
red.

## Interpreting Results

Log files from all the nodes should be compared in order to establish where the problem lies. There can be delays in
many parts of the system. By comparing log files from different nodes you can often match deltas in send times on one
node to deltas in receive times on the others. This can indicate that a sender is not properly scheduling its threads.
Deltas in receive times with no correlated deltas in send times can indicate a bottleneck in the network.

# Obtaining Meshmonitor

Meshmonitor is distributed as a compiled binary for Linux (x64). That is, a Java application
compiled to the native executable
using [GraalVM Community Edition](https://github.com/graalvm/graalvm-ce-builds/releases/). It has no additional
dependencies and can be run as is.

A pure Java version in jar form (meshmonitor.jar) is also available. The Java version should work on 
any platform with Java 8 or later installed (although it has only been tested on Linux).

# Using Meshmonitor

Meshmonitor strives to adhere to the industry standard [guidelines](https://clig.dev/#guidelines) on CLI design. The command
options can be printed using the `-h` parameter. This section describes basic usage and how the mesh is formed.

The central focus for this tool is the concept of a mesh: a set of connections between nodes such that each node
is connected to all others forming a [complete graph](https://en.wikipedia.org/wiki/Complete_graph):

```text

 ┌───(A)───┐
 │    │    │
(B)───┼───(D)
 │    │    │
 └───(C)───┘
```

Meshmonitor processes include the list of all known nodes in the mesh in the "ping" message. Through this mechanism
each node learns about all other nodes and a stable mesh is achieved after a few iterations of message exchange. The only
requirement is that each new meshmonitor needs to connect to at least one other that is already connected to the mesh.

The mesh is easy to create by simply starting all meshmonitor processes using a bind address by specifying the local machine’s *external* IP address (e.g., 192.161.0.3) and providing the IP address of one of the participating nodes as the first argument. For example:

```shell
# On the initial server (192.161.0.1), start meshmonitor with only the bind 
# address or List of servers to maintain permanent connection to:
$ ./meshmonitor -b 192.161.0.1

# On server 192.161.0.2 start meshmonitor and ask it to join 192.161.0.1
$ ./meshmonitor -b 192.161.0.2 192.161.0.1

# On server 192.161.0.3 start meshmonitor and ask it to join 192.161.0.1
$ ./meshmonitor -b 192.161.0.3 192.161.0.1
```

The meshmonitor processes can be killed and restarted and the mesh will heal. If a node goes down it is forgotten and
does not have to ever be restarted - the mesh keeps working. Adding a new node can be done at any time by
pointing a new meshmonitor process at one of the existing nodes.

NOTE: The IP passed to meshmonitor at startup is treated differently - meshmonitor will always try to reconnect to it.

## Openmetrics / Prometheus

Meshmonitor starts a simple web server on port 12223 that exposes Prometheus compatible metrics at the /metrics endpoint.
Optionally, it can be configured to bind to a non-default network interface using the `-m` option.

The /metrics endpoint can be disabled using the `-d` option.

### List of Metrics

Each metric contains two basic labels:

- `host_name` - the IP address of the host that meshmonitor is running on. This is the address passed to the `--bind`
  or `-b` option.
- `remote_host_name` - the IP address of the remote node that meshmonitor is communicating with. It's defined by the address passed to
  the `--bind` or `-b` option of the meshmonitor process running on the remote end.

Metrics contain three histograms for each host in the mesh and are encoded in
[Prometheus format](https://prometheus.io/docs/instrumenting/exposition_formats/). This means that each histogram is
defined by multiple metrics like `meshmonitor_receive_seconds_sum`, `..._count`, `..._bucket{}`.

Monitoring systems and their frontends like Grafana or Datadog know how to interpret histograms and will typically hide
individual metrics that define buckets and just expose a general histogram derived from them. These would
typically look like the following:

| Histogram | Metric as seen in Datadog/Grafana | Description                                                                                                                  |
|-----------|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| receive   | `meshmonitor_receive_seconds`     | Time between heartbeats. This is the main metric to look at. It should be close to the heartbeat interval.                   |
| delta     | `meshmonitor_delta_seconds`       | The difference between the timestamp encoded in the heartbeat and when the heartbeat was received.                           |
| send      | `meshmonitor_send_seconds`        | Time between *send* thread wakeups which should be close to 5ms. An ability of a thread to get scheduled in a timely manner. |

Histograms contain the following buckets: `10µs, 100µs, 500µs, 1ms, 2ms, 3ms, 4ms, 5ms, 6ms, 7ms, 8ms, 9ms, 10ms, 20ms, 30ms, 40ms, 50ms, 100ms, 200ms, 500ms, 1s, 2s, 5s, 10s, Inf+`.

## Datadog Monitoring

To use a locally running Datadog agent to scrape meshmonitor metrics create or
edit `/etc/datadog-agent/conf.d/openmetrics.d/conf.yaml` with following contents:

```yaml
init_config:
  service: 'meshmonitor'

instances:
  - openmetrics_endpoint: 'http://localhost:12223/metrics'
    namespace: 'meshmonitor'
    metrics: [ ".*" ]
    histogram_buckets_as_distributions: true
```

A Datadog dashboard specific to meshmonitor is available. You can import it
from [json file](dashboards/datadog.json).

## Running From a Jar

Use the following command to run meshmonitor from the jar file:

```shell
java -jar meshmonitor.jar <ARGS> 
```

Java 8 is enough to run it but Java 11 is required to build and execute tests.

## Building

Java SDK is required to build and test the Meshmonitor. Version 11 or above.
Maven is used as a build system but does not need to be installed locally.

The `mvnw` script (or `mvnw.cmd` on Windows) is used to bootstrap the build
and download required Maven runtime files. To build Meshmonitor and run all tests:

```shell
./mvnw clean install
```

to skip tests run:

```shell
./mvnw clean install -DskipTests
```
