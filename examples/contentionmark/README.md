# ContentionMark Example and Benchmark

ContentionMark is a small VoltDB application that measures VoltDB throughput under extreme contention.

How extreme? By default, ContentionMark creates one row, then updates it as fast as VoltDB will let it, one transaction per update.

We believe that no other system on the planet performs as well under contention as VoltDB, due to its unique architecture. We designed this app/benchmark to allow people to test us.

Note that the update is a read, increment, write operation, where the increment is between 0 and 4 (inclusive). This is slightly more difficult than a increment-by-one operation common to many counters.

It's also the smallest example we have, for what it's worth!


Variations to Try
-----------

- Change the disk persistence settings using a deployment.xml file. Try synchronous and asynchronous persistence. Try the app without persistence at all.
- Try with different replication settings.
- Change the number of contested keys by editing the run.sh file.
- Change the operation to be more complex than increment.
- Alter the default number (currently 8) of partition replicas per host using a deployment.xml file.

A more complex version of this app might populate the base table with billions of rows, but send 99% of updates to a single row. That might be a fun experiment for someone looking to learn VoltDB.

Quickstart
---------------------------
VoltDB examples come with a run.sh script that sets up some environment and saves some of the typing needed to work with Java clients. It is very readable and when executed, shows what is precisely being run to accomplish a given task..

1. Make sure "bin" inside the VoltDB kit is in your path.
2. Type "voltdb create -f" to start an empty, single-node VoltDB server.
3. Open a new shell in the same directory and type "sqlcmd < ddl.sql" to load the schema and the jarfile of procedures into VoltDB.
4. Type "./run.sh client" to run the client code.

You can stop the server or running client at any time with `ctrl-c` or `SIGINT`.

Note that the downloaded VoltDB kits include pre-compiled stored procedures and client code as jarfiles. To run the example from a source build, it may be necessary to compile the Java source code by typing "run.sh jars" before step 3 above. Note that this step requires a full Java JDK.

Other run.sh Actions
---------------------------
- *run.sh* : start the server
- *run.sh server* : start the server
- *run.sh init* : compile stored procedures and load the schema and stored procedures
- *run.sh jars* : compile all Java clients and stored procedures into two Java jarfiles
- *run.sh client* : start the client, more than 1 client is permitted
- *run.sh clean* : remove compilation and runtime artifacts
- *run.sh cleanall* : remove compilation and runtime artifacts *and* the two included jarfiles

If you change the client or procedure Java code, you must recompile the jars by deleting them in the shell or using `./run.sh jars`.

Client Behavior Options
---------------------------
You can control various characteristics of the demo by modifying the parameters passed into the java application in the "client" function of the run.sh script.

**Speed & Duration:**

    --displayinterval=5           (seconds between status reports)
    --warmup=5                    (how long to warm up before measuring
                                   benchmark performance.)
    --duration=120                (benchmark duration in seconds)
    --ratelimit=20000             (run up to this rate of requests/second)

**Cluster Info:**

    --servers=$SERVERS            (host(s) client connect to, e.g.
                                   =localhost
                                   =localhost:21212
                                   =volt9a,volt9b,volt9c
                                   =foo.example.com:21212,bar.example.com:21212)

**Parameters Affecting Simulation:**

    --tuples=1                    (number of tuples to contend for)


Customizing this Example
---------------------------
See the "deployment-examples" directory within the "examples" directory for ways to alter the default single-node, no authorization deployment style of the examples. There are readme files and example deployment XML files for different clustering, authorization, export, logging and persistence settings.
