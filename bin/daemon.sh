#!/bin/sh
ROOT=`pwd`

exec java \
    -classpath "$ROOT/lib/*:$ROOT/lib/javagat/*:$ROOT/lib/ipl/*" \
    -Dlog4j.configuration=file:$ROOT/log4j.properties \
    -Dgat.adaptor.path=$ROOT/lib/javagat/adaptors \
    sc11.daemon.Daemon --grid $1 --verbose
