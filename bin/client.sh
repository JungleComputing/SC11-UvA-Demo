#!/bin/sh

ROOT=`pwd`

java -cp "$ROOT/lib/sc11-client-0.2.0.jar" sc11.client.Client --input $1 --filetype .jpg --output $2 --filter OP1 --filter OP2 --filter OP3 --daemon localhost --site carrot --nodes 2
