# Scala incompatibility in the Flink 1.18.1/Java 17 Docker image

## Expected behavior

The Scala API should still work in Flink 1.18. The small test program in this repository produces a jar with

```
sbt assembly
```

This generates a file `target/scala-2.12/scala-java17-flink-assembly-0.1.0-SNAPSHOT.jar` that can be uploaded via the Flink Job Manager UI in a web browser or from the command line.

When submitted on a Flink session cluster, this test should simply print a random number to the console once per second. The numbers show up in the task manager logs something like this:

```
222750e9c0_0_0) switched from INITIALIZING to RUNNING.
We sank the item 2097057567
We sank the item 744841796
We sank the item 790577660
We sank the item 1747875335
We sank the item 1604673357
We sank the item 177711332
We sank the item 1078458897
We sank the item 1136606281
We sank the item 34207968
We sank the item 1238144389
...
```

This output is seen when running with the Java 11 Flink 1.18.1 Docker image.

## Scala API failure under Java 17
Start a task manager and a job manager using the `flink:1.18.1-scala_2.12-java17` image instead of `flink:1.18.1-scala_2.12-java11`.

When the same job is submitted, there's an immediate exception logged on the task manager:

```
222750e9c0_0_0) switched from INITIALIZING to FAILED with failure cause:
org.apache.flink.streaming.runtime.tasks.StreamTaskException: Cannot instantiate user function.
	at org.apache.flink.streaming.api.graph.StreamConfig.getStreamOperatorFactory(StreamConfig.java:405) ~[flink-dist-1.18.1.jar:1.18.1]
...
Caused by: java.io.InvalidObjectException: ReflectiveOperationException during deserialization
	at java.lang.invoke.SerializedLambda.readResolve(Unknown Source) ~[?:?]
...
Caused by: java.lang.reflect.InvocationTargetException
	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]
...
Caused by: java.lang.IllegalArgumentException: too many arguments
	at java.lang.invoke.LambdaMetafactory.altMetafactory(Unknown Source) ~[?:?]
	at scala.runtime.LambdaDeserializer$.makeCallSite$1(LambdaDeserializer.scala:93) ~[flink-scala_2.12-1.18.1.jar:1.18.1]
	at scala.runtime.LambdaDeserializer$.deserializeLambda(LambdaDeserializer.scala:102) ~[flink-scala_2.12-1.18.1.jar:1.18.1]
	at scala.runtime.LambdaDeserialize.deserializeLambda(LambdaDeserialize.java:26) ~[flink-scala_2.12-1.18.1.jar:1.18.1]
...
```

The Scala compiler prior to 2.12.15 had [a bug](https://github.com/scala/bug/issues/12419) in the LambdaDeserializer code that causes these exceptions when running on Java 17.

## Fixing the Scala API on Java 17

It appears that the `flink-scala_2.12-1.18.1.jar` included in the official Docker image was built with a too-old Scala compiler.

A working replacement can be built like so:

```
git clone https://github.com/apache/flink.git
cd flink
git checkout release-1.18.1
sed -i '' -e 's@BoxesRunTime@//BoxesRunTime@g' ./flink-end-to-end-tests/flink-end-to-end-tests-scala/src/main/scala/org/apache/flink/tests/scala/ScalaJob.scala
mvn clean install package -DskipTests -Dfast -Pskip-webui-build -T 1C -Dscala.version=2.12.15
```

The key is `-Dscala.version=2.12.15` to ensure that the flink-scala code is built with a Java 17-compatible compiler. Once the `flink-scala_2.12-1.18.1.jar` inside the container has been replaced with the rebuilt one, the test job runs successfully just as it does under Java 11.

### Starting a Flink session cluster on Java 11
For the job manager:

```
docker run \
    --rm \
    --name=jobmanager \
    --network flink-network \
    --publish 8081:8081 \
    --env FLINK_PROPERTIES="jobmanager.rpc.address: jobmanager" \
    flink:1.18.1-scala_2.12-java11 jobmanager
```

For the task manager:

```
docker run \
    --rm \
    --name=taskmanager \
    --network flink-network \
    --env FLINK_PROPERTIES="jobmanager.rpc.address: jobmanager" \
    flink:1.18.1-scala_2.12-java11 taskmanager
```

### Starting a Flink session cluster on Java 17

For the job manager:

```
docker run \
    --rm \
    --name=jobmanager \
    --network flink-network \
    --publish 8081:8081 \
    --env FLINK_PROPERTIES="jobmanager.rpc.address: jobmanager" \
    flink:1.18.1-scala_2.12-java17 jobmanager
```

For the task manager:
```
docker run \
    --rm \
    --name=taskmanager \
    --network flink-network \
    --env FLINK_PROPERTIES="jobmanager.rpc.address: jobmanager" \
    flink:1.18.1-scala_2.12-java17 taskmanager
```
