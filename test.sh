#!/bin/bash
docker-compose up -d

sbt test -DRABBIT_HOST=boot2docker
docker-compose kill
docker-compose rm -f
