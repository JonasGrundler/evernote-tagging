#!/bin/bash

(
    cd ../docker
    docker run -v ~:/data python-services
)
