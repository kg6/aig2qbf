#!/bin/sh

SCRIPT_PATH=`dirname $0`; SCRIPT_PATH=`eval "cd $SCRIPT_PATH && pwd"`

# test
echo 

"$SCRIPT_PATH/../tools/aigdd" -v $1 $2 $SCRIPT_PATH/../scripts/check.pl $3 
