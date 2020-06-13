#!/bin/bash
# Runs the benchmarks with selected OpenJDK builds
trap exit INT TERM
set -o errexit

# System environment
host=$(hostname --short)
code=$(lsb_release --short --codename)
arch=$(dpkg --print-architecture)
date=$(date --iso-8601)

# JavaFX SDK libraries
if [ "$arch" = "armhf" ]; then
    javafxlib=$HOME/lib/armv6hf-sdk/lib
else
    javafxlib=$HOME/lib/javafx-sdk-15/lib
fi

# Benchmark application
jarfile=target/benchmarks.jar

# Ubuntu builds
ubuntu11=/usr/lib/jvm/java-11-openjdk-$arch
ubuntu13=/usr/lib/jvm/java-13-openjdk-$arch
ubuntu14=/usr/lib/jvm/java-14-openjdk-$arch

# Oracle builds
oracle14=$HOME/opt/jdk-14.0.1

# AdoptOpenJDK builds
adopt11=$HOME/opt/jdk-11.0.7+10
adopt12=$HOME/opt/jdk-12.0.2+10
adopt13=$HOME/opt/jdk-13.0.2+8
adopt14=$HOME/opt/jdk-14.0.1+7

# Example: writeTo..New
filters=""

# Example: -f 1 -i 1 -wi 1 -r 60s -w 30s
options=""

# Example: -verbose:gc -Xlog:gc* -XX:+PrintCompilation
# Example: -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -XX:+LogCompilation
# Example: -XX:StartFlightRecording=settings=profile,filename=profile.jfr
# Example: -agentpath:/path/to/libasyncProfiler.so=start,file=profile.svg
jvmargs="-Djava.library.path=$javafxlib"

jdklist="$ubuntu11 $ubuntu13 $ubuntu14"
for jdk in $jdklist; do
    printf "\n[$(date)] Testing $jdk ...\n"
    jdkbase=$(basename $jdk | tr '+' '_')
    logname=${host}-${code}-${jdkbase}-${date}
    $jdk/bin/java -version
    time $jdk/bin/java -jar $jarfile $filters $options -o ${logname}.log \
        -rf text -rff ${logname}.txt -jvmArgs "$jvmargs"
done
