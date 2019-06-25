#!/bin/bash
# Updates the local OpenJFX Maven artifacts from the build machine
trap exit INT TERM
set -o errexit

dir=.m2/repository/org/openjfx
rsync -av --delete buildjfx64:$dir/ $HOME/$dir/
