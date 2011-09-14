#!/bin/sh

ROOT=`pwd`

java -cp "$ROOT/lib/sc11-client-0.2.0.jar" sc11.client.Client --inputURI $1 --inputSuffix .jpg --outputURI $2 --filter OP1 --filter OP2 --filter OP3 --daemon localhost --site carrot --nodes 2
