#!/bin/sh
# upload_script.sh

SRC=$1
TITLE=
if [ $# = 2 ]; then
    TITLE=$2
else
    TITLE=`basename -s .pdf "$SRC"`
fi
TOPDIR=`pwd`

TMPSRC=`mktemp /tmp/src_XXXXX`
OUTDIR=`mktemp -d /tmp/messtin_XXXXX`

cp "$SRC" $TMPSRC

split() {
	# setup
	# cp "$SRC" $TEMPSRC

	# split
	gs -dBATCH -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -dNOPAUSE -sDEVICE=jpeg -r200 -sOutputFile=$OUTDIR/%03d.jpg $TMPSRC
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
	cd $TOPDIR
}

upload() {
	cd $TOPDIR
	ruby upload.rb "$TITLE" $OUTDIR
}

cleanup() {
	rm $TMPSRC
	rm -r $OUTDIR
}

split
thumbnails
upload
cleanup
