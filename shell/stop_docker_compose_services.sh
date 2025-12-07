#!/bin/bash

# Auszuführen im docker-Verzeichnis (dort, wo das script liegt)
(
    cd ../docker
    docker compose -p evernote-services stop
)