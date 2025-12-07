#!/usr/bin/env bash

set -e

DATA=~/data

(
    export DATA
    cd "../java/runner"
    mvn  -DskipTests exec:java@parse-enex
    mvn  -DskipTests exec:java@build-csv
)
