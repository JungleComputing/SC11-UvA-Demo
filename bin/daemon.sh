#!/bin/sh
ROOT=`pwd`

exec java \
    -classpath "$ROOT/lib/*:$ROOT/lib/JavaGAT-2.1.1/*" \
    -Dlog4j.configuration=file:$ROOT/log4j.properties \
    -Dgat.adaptor.path=$ROOT/lib/JavaGAT-2.1.1/adaptors \
    sc11.daemon.Daemon --grid $1 --verbose
