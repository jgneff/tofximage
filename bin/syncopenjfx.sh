#!/bin/bash
# Updates the local repository with the latest OpenJFX artifacts
#
# First publish the artifacts locally on the build machine with:
#   $ gradle publishToMavenLocal
# Add javafx-graphics as a dependency and run with:
#   $ java -Djava.library.path=$HOME/lib/javafx-sdk-14-dev/lib \
#       -jar target/benchmarks.jar
trap exit INT TERM
set -o errexit

host=buildjfx64
dir=.m2/repository/org/openjfx
rsync -av $host:$dir/ $HOME/$dir/
