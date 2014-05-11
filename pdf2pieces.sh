#!/bin/zsh

SRC=$1
OUTDIR=$2
TITLE=`basename -s .pdf $SRC`

echo $TITLE

split() {
	# setup
	cp "$SRC" src.pdf
	mkdir -p $OUTDIR

	# split
	gs -dBATCH -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -dNOPAUSE -sDEVICE=jpeg -r200 -sOutputFile=$OUTDIR/%03d.jpg src.pdf
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
thumbnails