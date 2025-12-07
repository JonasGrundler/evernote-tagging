#!/bin/bash

(
    cd ../java/runner
    mvn clean package -DskipTests
)