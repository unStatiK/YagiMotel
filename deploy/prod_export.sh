#!/bin/bash

export YAGI_MOTEL_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ConcGCThreads=2 -Xms256M -Xmx512M"
