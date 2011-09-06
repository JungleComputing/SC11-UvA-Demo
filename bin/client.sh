#!/bin/sh

ROOT=/home/jason/workspace/SC11-UvA-Demo

java -cp "./lib/*" sc11.Client --input $ROOT/images --filetype .jpg --output $ROOT/output --filter OP1 --filter OP2 --filter OP3 --server localhost
