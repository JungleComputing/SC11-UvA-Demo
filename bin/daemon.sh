#!/bin/sh
ROOT=/home/jason/Workspace/SC11-UvA-Demo

exec java \
    -classpath "$ROOT/lib-daemon/"'*' \
    -Dlog4j.configuration=file:$ROOT/log4j.properties \
    -Dgat.adaptor.path=./lib-daemon/adaptors \
    sc11.daemon.Daemon --grid $1 --verbose
