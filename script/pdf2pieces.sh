#!/bin/zsh

SRC=$1
TEMPSRC=$2
OUTDIR=$3

split() {
	# setup
	# cp "$SRC" $TEMPSRC

	# split
	echo split
	echo $TEMPSRC
	echo $OUTDIR
	gs -dBATCH -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -dNOPAUSE -sDEVICE=jpeg -r200 -sOutputFile=$OUTDIR/%03d.jpg $TEMPSRC
}

thumbnails() {
	# create thumbnails
	cd $OUTDIR
	mkdir tm

	for f in *.jpg; do
	  sips -z 128 96 $f --out tm/$f
	done

	# copy the first page as a cover page
	cp tm/001.jpg cover.jpg
}

split
#thumbnails