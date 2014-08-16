#!/bin/sh
# upload_script.sh

SRC=$1
TITLE=$2
OBJID=
PAGES=0

TOPDIR=`pwd`

TMPSRC=`mktemp /tmp/src_XXXXX`
OUTDIR=`mktemp -d /tmp/messtin_XXXXX`

PARSE_APP_ID=`jq '.app_id' script/secret.json`
PARSE_CLIENT_KEY=`jq '.client_key' script/secret.json`
SERVER_URL=`jq '.storage_server' script/secret.json`

cp "$SRC" $TMPSRC

split() {
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

count_pages() {
    PAGES=`ls $OUTDIR/tm | wc -l`
    echo pages = $PAGES
}

post_to_parse_com() {
    OBJID=`curl -X POST \
        -H "X-Parse-Application-Id: $PARSE_APP_ID" \
        -H "X-Parse-REST-API-Key: $PARSE_CLIENT_KEY" \
        -H "Content-Type: application/json" \
        -d "{\"title\":\"$TITLE\",\"pages\":$PAGES}" \
        https://api.parse.com/1/classes/Book \
    | ruby -ane 'require("json"); puts JSON.parse($F[0])["objectId"];'`
    echo objid = $OBJID
}

upload() {
    cd $TOPDIR
    echo "rsync -av $OUTDIR/ $SERVER_URL:messtin_storage/$OBJID/"
    rsync -av $OUTDIR/ $SERVER_URL:messtin_storage/$OBJID/
}


cleanup() {
    rm $TMPSRC
    rm -r $OUTDIR
}

split
thumbnails
count_pages
post_to_parse_com
upload
cleanup
