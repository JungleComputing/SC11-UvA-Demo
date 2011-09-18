#!/bin/sh

ROOT=`pwd`

java -cp "$ROOT/lib/sc11-client-0.2.0.jar" sc11.client.Client --inputURI $1 --inputSuffix .tif --outputURI $2 --filter TIF_JPG --filter SCALE --daemon localhost --site $3 --nodes $4
