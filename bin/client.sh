#!/bin/sh

ROOT=/home/jason/Workspace/SC11-UvA-Demo

java -cp "$ROOT/deploy-workspace/lib-daemon/*" sc11.daemon.Client --input $ROOT/images --filetype .jpg --output $ROOT/output --filter OP1 --filter OP2 --filter OP3 --daemon localhost --site carrot --nodes 2
