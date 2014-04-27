// book.js

var sqlite3 = require('sqlite3');
/*
var redis = require('redis').createClient();
redis.on("error", function (err) {
  console.log("Error " + err);
});
*/
var db = new sqlite3.Database('messtin.db');

var Book = function() {};

Book.list = function(req, res) {
  console.log('list');
  /*
  redis.llen('books', function(err, len) {
    console.log('book 1 ' + err + ', ' + len);
    if (len == 0) {
      redis.lpush('books', 'yay', function(err2, obj) {
        console.log('book 2');
      })
    } else {
      redis.lrange('books', 0, len, function(err2, arr) {
        if (err2) {
          console.log('error... ' + err2);
        } else {
          console.log('ho! ' + arr.length);
          console.log(arr[0]);
        }
      })
    }
  });
*/
  db.all('select * from book', function(err, rows) {
    res.send(rows);
  });
};


exports.list = function(req, res) {
  Book.list(req, res);
};

exports.upload = function(req, res) {
  res.send("uploading a book");
  // decompose a PDF file
  //
  // save the src file into tmp dir
  // spwan(sh pdf2jpegs.sh $tmp/src.pdf $tmp/out)
  // for j in $tmp/out/*.jpg
  //   make thumbnail image
  //   upload $j to GDrive
  //   upload $j-thumbnail to GDrive
  // end
  //
  // Retrieve the meta data for this file (Book) from GDrive
  // Notify the client when all upload finished, or there is an error.
  //
  // save those fragments to the starge server
  // record the location of the files in the storage server
  // record the metadata
  // return
};

//
// will return a book info:
//   title
//   # of pages
//   base URL
//   last opened date
//   (cover image url)
// takes book ID (bid) as a parameter
exports.info_redis = function(req, res) {
  console.log('book info about ' + req.params.bid);
  redis.get('book:' + req.params.bid, function(err, bookInfo) {
    console.log('bookInfo: ' + bookInfo);
    res.send(bookInfo);
  });
}

exports.info = function(req, res) {
  db.get('select * from book where id=?', req.params.bid, function(err, row) {
    res.send(row);
  });
}
