#!/bin/sh

ROOT=/home/jason/workspace/SC11-UvA-Demo
./bin/ipl-run -Dibis.pool.name=TEST -Dibis.server.address=192.168.1.101-8888 sc11.Slave --scriptdir $ROOT/scripts --tmpdir $ROOT/tmp --exec slave 1 --exec gpu 1
