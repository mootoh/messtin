// List up directories in messtin/bundle in GDrive, retrieve meta data, and store them in SQLite db.

var googleapis = require('googleapis'),
  readline = require('readline'),
  fs = require('fs'),
  auth_wrap = require('./auth_wrap'),
  Parse = require('node-parse-api').Parse;

var parse_secret = JSON.parse(fs.readFileSync('./parse_secret.json'));
var app = new Parse(parse_secret.app_id, parse_secret.master_key);

var books = [];

function list(error, client, auth) {
  console.log('getting list');

  function get_root_folder_id(callback) {
    client.drive.files.list({
      q: 'title = "messtin"'
    }).withAuthClient(auth).execute(function(error, response) {
      var messtin_id = response.items[0].id;
      console.log('messtin.id = ' + messtin_id);

      callback(messtin_id);
    });
  }

  var pages_count = 0;
  var covers_count = 0;

  function isFinish() {
      return (pages_count == books.length && covers_count == books.length);
  }

  function doneIfFinished() {
      if (isFinish()) {
          //console.log(books);
          console.log('finished retrieving books from GDrive');

          var parse_count = 0;

          books.forEach(function(book) {
              console.log('inserting a book into Parse.com');
              app.insert('Book', {
                  title: book.title,
                  pages: book.pages,
                  gd_id: book.id,
                  cover_img_gd_id: book.cover_img_gd_id
              }, function(err, response) {
                  if (err) {
                      console.log('failed in inserting a book in Parse.com: ' + err);
                  }
                  if (parse_count++ == books.length) {
                      console.log('done.');
                      process.exit();
                  }
              });
          });

      }
  }

  get_root_folder_id(function(bundle_id) {
    client.drive.files.list({
      q: "'" + bundle_id + "' in parents"
    }).withAuthClient(auth).execute(function(error, response) {
      response.items.forEach(function(item) {
        if (item.labels.trashed)
          return;
        console.log(item.title + ', ' + item.id);

        books.push(item);

        client.drive.files.list({
          q: "mimeType = 'image/jpeg' and '" + item.id + "' in parents",
          maxResults: 1000
        }).withAuthClient(auth).execute(function(error, response) {
          console.log(item.title + ': pages = ' + response.items.length);
          item.pages = response.items.length;

          pages_count++;
          doneIfFinished();
        });

        client.drive.files.list({
          q: "title = 'cover.jpg' and '" + item.id + "' in parents"
        }).withAuthClient(auth).execute(function(error, response) {
          if (error || response.items.length == 0) {
            console.log(item.title + ' is not ready, skipping');
            return;
          }
          var cover_img_gd_id = response.items[0].id;
          console.log(item.title + ': cover_img_gd_id = ' + cover_img_gd_id);
          item.cover_img_gd_id = cover_img_gd_id;

          covers_count++;
          doneIfFinished();
        });
      });
    });
  });
}

auth_wrap.prepareGoogleApi(list);
