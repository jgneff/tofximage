#!/bin/bash
# Guide to installing 3rd party JARs
# https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
#
# After running this script, right-click the JAR file under Dependencies
# in the NetBeans Projects view and manually install the artifact again.
trap exit INT TERM
set -o errexit

export JAVA_HOME=$HOME/opt/jdk-12.0.1
sdk=$HOME/lib/javafx-sdk-13-dev
mvn=/snap/netbeans/current/netbeans/java/maven/bin/mvn

$mvn -version

jar -cf $sdk/src/javafx-base-13-dev-sources.jar -C $sdk/src/javafx.base .
jar -cf $sdk/src/javafx-graphics-13-dev-sources.jar -C $sdk/src/javafx.graphics .

$mvn install:install-file -DgeneratePom=false -Dfile=$sdk/lib/javafx.base.jar \
    -DgroupId=org.openjfx -DartifactId=javafx-base -Dversion=13-dev -Dpackaging=jar
$mvn install:install-file -DgeneratePom=false -Dfile=$sdk/src/javafx-base-13-dev-sources.jar \
    -DgroupId=org.openjfx -DartifactId=javafx-base -Dversion=13-dev -Dpackaging=jar

$mvn install:install-file -DgeneratePom=false -Dfile=$sdk/lib/javafx.graphics.jar \
    -DgroupId=org.openjfx -DartifactId=javafx-graphics -Dversion=13-dev -Dpackaging=jar
$mvn install:install-file -DgeneratePom=false -Dfile=$sdk/src/javafx-graphics-13-dev-sources.jar \
    -DgroupId=org.openjfx -DartifactId=javafx-graphics -Dversion=13-dev -Dpackaging=jar
