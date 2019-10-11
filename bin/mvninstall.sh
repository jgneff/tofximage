#!/bin/bash
# Installs the OpenJFX SDK artifacts into the local repository
#
# Guide to installing 3rd party JARs
# https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
#
# Add javafx-base and javafx-graphics as dependencies and run with:
#   $ java -Djava.library.path=$HOME/lib/javafx-sdk-14-dev/lib \
#       -jar target/benchmarks.jar
# This script assumes the OpenJFX sources have been extracted with:
#   $ unzip -q $HOME/lib/javafx-sdk-14-dev/lib/src.zip \
#       -d $HOME/lib/javafx-sdk-14-dev/src
trap exit INT TERM
set -o errexit

sdk=$HOME/lib/javafx-sdk-14-dev
ver=14-dev

for mod in base controls fxml graphics media swing web; do
    jar -cf $sdk/src/javafx.$mod.jar -C $sdk/src/javafx.$mod .
    mvn install:install-file -Dfile=$sdk/lib/javafx.$mod.jar \
        -Dsources=$sdk/src/javafx.$mod.jar -Dpackaging=jar \
        -DgroupId=org.openjfx -DartifactId=javafx-$mod -Dversion=$ver
done
