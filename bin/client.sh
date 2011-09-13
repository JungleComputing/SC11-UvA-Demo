#!/bin/sh

ROOT=`pwd`

java -cp "$ROOT/lib/sc11-client-0.2.0.jar" sc11.daemon.Client --input $ROOT/images --filetype .jpg --output $ROOT/output --filter OP1 --filter OP2 --filter OP3 --daemon localhost --site carrot --nodes 2
