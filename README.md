# Overview

Meshmonitor is a tool for monitoring network issues such as network delays and instability, mysterious timeouts, hangs,
and scheduling problems that delay message passing.

While it can be used as a general network monitoring tool it simulates the VoltDB heartbeat paths and is intended to
diagnose situations when sites are experiencing dead host timeouts without any obvious network event.

Meshmonitor can be run alongside VoltDB. It has very low overhead on the CPUs and network. It can also be helpful to run
without VoltDB - as a proof point that your environment is going to have adequate baseline stability for running VoltDB.

# What Meshmonitor Can (and Can't) Tell You

First - an analogy. Think of VoltDB as a fast car. Like all cars, the quality of the streets impacts the top speed at
which you can travel. If the streets you drive on are poorly paved, have random lane closings, or a lot of traffic
lights then your fast car goes no faster than any old jalopy.

Environmental blips in scheduling, networking, CPU accesses are like potholes to VoltDB. If a VoltDB site thread can't
run for 50ms - then your application can experience long-tail latency problems. If the heart beating between cluster
nodes is delayed for long enough, then some nodes may get ejected from the cluster.

The following is an ever-growing list of things that the VoltDB support team has seen when looking at customers' Mesh
Monitor data:

1. Batch network copies/backups that are doing a high IO load that linux decides is more important than scheduling
   VoltDB (a solution is to throttle these jobs)
2. Other processes on the system taking too much CPU
3. VMs/Containers that are starved by neighbors
4. VMs/Containers with incorrect/inadequate thread pinning
5. VM/Containers that are migrating
6. Power save modes that slow down "idle" processors
7. Boot/grub kernel setting of idle=poll
8. Network congestion between particular nodes

Causes of latency/timeouts that are not visible using meshmonitor (but may be visible in volt.log):

1. GC stalls
2. VoltDB memory compaction delays

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

# Output

All messages printed by Meshmonitor contain event time (`HH:mm:ss`) and an IP address of the node that the message
pertains to. If message has no such context then IP column will be empty:

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
the `-p`ing interval in mind (default 5ms) that is included in the measurement values. The values that are printed are
max, mean, and percentiles: 99th, 99.9th, and 99.99th:

```console
09:14:11 [               ] --------receive-(ms)--------- ---------delta-(ms)---------- ---------send-(ms)-----------
09:14:11 [               ]   Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99
09:14:11 [   172.31.10.72]   5.2   5.1   5.1   5.2   5.2|  0.2   0.0   0.0   0.1   0.2|  5.1   5.1   5.1   5.1   5.1
09:14:11 [    172.31.14.3]   5.3   5.1   5.1   5.1   5.3|  0.4   0.2   0.2   0.2   0.4|  5.8   5.1   5.1   5.5   5.8
09:14:11 [   172.31.9.146]   5.1   5.1   5.1   5.1   5.1|  5.1   2.6   5.0   5.1   5.1|  5.1   5.1   5.1   5.1   5.1
09:14:11 [   172.31.5.177]   5.1   5.1   5.1   5.1   5.1|  5.2   2.8   5.2   5.2   5.2|  5.1   5.1   5.1   5.1   5.1
```

Measurements exceeding `--threshold` (default 20ms) will be printed in yellow. These that exceed 1s will be printed in
red.

# Interpreting Results

Log files from all the nodes should be compared in order to establish where the problem lies. There can be delays in
many parts of the system. By comparing log files from different nodes you can often match deltas in send times on one
node to deltas in receive times on the others. This can indicate that a sender is not properly scheduling its threads.
Deltas in receive times with no correlated deltas in send times can indicate a bottleneck in the network.

# Obtaining Meshmonitor

Primary distribution form of Meshmonitor is a compiled binary for Linux (x64). In this form it is a Java application
compiled to the native executable
using [GraalVM Community Edition](https://github.com/graalvm/graalvm-ce-builds/releases/). It has no additional
dependencies and can be run as is.

Pure Java version in jar form (meshmonitor.jar) is also available, and it will work on any platform with installed Java
21 or later (but was only tested on Linux).

# Using Meshmonitor

Meshmonitor strives to adhere to the industry standard [guidelines](https://clig.dev/#guidelines) on CLI design. It's
various options can be printed using `-h` parameter. This section describes basic usage and how the mesh is formed.

The central part to operation of this tool is the concept of mesh - set of connections between nodes such that each node
is connected to all others forming a [complete graph](https://en.wikipedia.org/wiki/Complete_graph).

```mermaid
graph TD;
    A-->B;
    A-->C;
    B-->D;
    C-->D;
```

```
Usage: ./meshmonitorhelper.sh NODESFILE <HICCUPSIZE> <LOGINTERVAL> <NETWORKPORT>
   NODESFILE - required parameter         - file with list of nodes on each line
   <HICCUPSIZE>  - optional               - mininum latency in milliseconds to report, default value = 20
   <LOGINTERVAL> - optional               - interval of logging in seconds, default value = 10
   <NETWORKPORT> - optional               - network port used, default value = 12222

```

Most customers run with the default HICCUPSIZE and LOGINTERVAL.

Sample output for a nodes.txt that lists 3 nodes:  
prod1  
prod2  
client1

```
> ./meshmonitorhelper.sh nodes.txt
Using list of hosts file: nodes.txt
Using default minimum hiccup size: 20
Using default logging interval in seconds: 10
Using default network port: 12222

Generating the commands needed to run on all other nodes:

#prod1: In <VOLTDB_HOME>/tools/meshmonitor directory, run the following command:
nohup java -jar meshmonitor.jar 20 10 prod1:12222 > prod1-mesh.log &

#prod2: In <VOLTDB_HOME>/tools/meshmonitor directory, run the following command:
nohup java -jar meshmonitor.jar 20 10 prod2:12222 prod1:12222 > prod2-mesh.log &

#client1: In <VOLTDB_HOME>/tools/meshmonitor directory, run the following command:
nohup java -jar meshmonitor.jar 20 10 client1:12222 prod2:12222 prod1:12222 > client1-mesh.log &
```

# Openmetrics / Prometheus

Meshmonitor starts a simple web server on port 12223 that exposes Prometheus compatible metrics under /metrics endpoint.
It can be further configured to bind to non default network interface using `-m` option.

This functionality can be disabled using `-d` option.

# Datadog monitoring

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
