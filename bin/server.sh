#!/bin/sh
ROOT=/home/jason/workspace/SC11-UvA-Demo

./bin/ipl-run -Dibis.pool.name=TEST -Dibis.server.address=192.168.1.101-8888 -Dgat.adaptor.path=./lib/adaptors sc11.Master --exec 1 --config $ROOT/scripts/configuration --scriptdir $ROOT/scripts --tmpdir $ROOT/tmp
