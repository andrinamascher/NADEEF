#!/bin/bash

if [ -f javac ]; then
    echo JDK cannot be found, please first check your PATH.
else if ! [ -d "out" ]; then
    echo Nadeef is not yet compiled, please first run 'ant' to build it.
else
    export BuildVersion='1.0.974'
    cmd='java -d64 -Xmx2048M -cp out/nadeef.jar:out/production:.:examples/ qa.qcri.nadeef.console.Console'
    exec $cmd
fi
fi
