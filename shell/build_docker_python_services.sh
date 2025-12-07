#!/bin/bash

# Auszuführen im docker-Verzeichnis (dort, wo das script liegt)
(
    cd ../docker
    docker build -f Dockerfile.python-services -t python-services ../
)