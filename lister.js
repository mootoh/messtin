// List up directories in messtin/bundle in GDrive, retrieve meta data, and store them in SQLite db.

var googleapis = require('googleapis'),
  readline = require('readline'),
  fs = require('fs');
  auth_wrap = require('./auth_wrap');

var sqlite3 = require('sqlite3');
var db = new sqlite3.Database('messtin.db');

function list(error, client, auth) {
  console.log('getting list');

  function get_bundle_id(callback) {
    client.drive.files.list({
      q: 'title = "messtin"'
    }).withAuthClient(auth).execute(function(error, response) {
      var messtin_id = response.items[0].id;
      console.log('messtin.id = ' + messtin_id);

      client.drive.files.list({
        q: "title = 'bundle' and '" + messtin_id + "' in parents"
      }).withAuthClient(auth).execute(function(error2, response2) {
        var bundle_id = response2.items[0].id;
        console.log('bundle.id = ' + bundle_id);
        callback(bundle_id);
      });
    });
  }

  get_bundle_id(function(bundle_id) {
    client.drive.files.list({
      q: "'" + bundle_id + "' in parents"
    }).withAuthClient(auth).execute(function(error, response) {
      response.items.forEach(function(item) {
        if (item.labels.trashed)
          return;
        console.log(item.title);

        save_if_new(item);
      });
    });
  });

  function save_if_new(item, callback) {
    var title = item.title;
    var gd_id = item.id;

    db.get('SELECT * from book WHERE gd_id = ?', gd_id, function(err, row) {
      console.log('select for gd_id=' + gd_id + ', err =' + err + ', row=' + row);
      if (row) {
        console.log(item.title + ' already exists in DB, skipping');
        return;
      }

      client.drive.files.list({
        // q: "title contains 'jpg' and '" + gd_id + "' in parents"
        q: "mimeType = 'image/jpeg' and '" + gd_id + "' in parents"
      }).withAuthClient(auth).execute(function(error, response) {
        console.log(item.title + ': pages = ' + response.items.length);
        var pages = response.items.length-1;

        client.drive.files.list({
          q: "title = 'cover.jpg' and '" + gd_id + "' in parents"
        }).withAuthClient(auth).execute(function(error2, response2) {
          if (error2 || response2.items.length == 0) {
            console.log(title + ' is not ready, skipping');
            return;
          }
          var cover_img_gd_id = response2.items[0].id;

          console.log(title + ' inserting a row...');

          db.run('INSERT INTO book (title, pages, gd_id, cover_img_gd_id) VALUES (?, ?, ?, ?)',
              title, pages, gd_id, cover_img_gd_id);
        });
      });
    });
  }
}

auth_wrap.prepareGoogleApi(list);