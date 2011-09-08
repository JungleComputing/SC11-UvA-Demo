#!/bin/sh
ROOT=/home/jason/Workspace/SC11-UvA-Demo/deploy-workspace

exec java \
    -classpath "$ROOT/lib-daemon/"'*' \
    -Dlog4j.configuration=file:$ROOT/log4j.properties \
    -Dgat.adaptor.path=$ROOT/lib-daemon/adaptors \
    sc11.daemon.Daemon --grid $1 --verbose
