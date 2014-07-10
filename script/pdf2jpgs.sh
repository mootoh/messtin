#!/bin/sh

SRC=$1
OUTDIR=$2

gs -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -dNOPAUSE -sDEVICE=jpeg -r200 -sOutputFile=$OUTDIR/%03d.jpg $SRC
