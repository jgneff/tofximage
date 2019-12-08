#!/bin/bash
# Runs the benchmarks on Ubuntu armhf

# Ubuntu builds of JDK 11 and 13
# JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf
# JAVA_HOME=/usr/lib/jvm/java-13-openjdk-armhf

# AdoptOpenJDK build of JDK 13
JAVA_HOME=$HOME/opt/jdk-13.0.1+9

JAVA_LIB=$HOME/lib/armv6hf-sdk/lib
JAVA_JAR=$HOME/lib/benchmarks.jar

$JAVA_HOME/bin/java -Djava.library.path=$JAVA_LIB \
    -jar $JAVA_JAR $@
