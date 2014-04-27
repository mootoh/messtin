// gapi_wrap.js

var googleapis = require('googleapis'),
  fs = require('fs');

function prepareGoogleApi(callback) {
  googleapis.discover('drive', 'v2').execute(function(err, client) {
    if (err) {
      callback(err);
      return;
    }

    var REDIRECT_URL = 'urn:ietf:wg:oauth:2.0:oob',
      SCOPE = 'https://www.googleapis.com/auth/drive';
    var credential = JSON.parse(fs.readFileSync('credential.json'));
    var auth = new googleapis.OAuth2Client(credential.CLIENT_ID, credential.CLIENT_SECRET, REDIRECT_URL);
    var url = auth.generateAuthUrl({ scope: SCOPE });

    // if a new token required
    if (credential['tokens']) {
      auth.credentials = credential.tokens;
      callback(null, client, auth);
      return;
    }

    function getAccessToken(code) {
      auth.getToken(code, function(err, tokens) {
        if (err) {
          console.log('Error while trying to retrieve access token', err);
          callback(err);
          return;
        }
        auth.credentials = tokens;
        credential['tokens'] = tokens;
        fs.writeFileSync('./credential.json', JSON.stringify(credential));

        callback(null, client, auth);
      });
    };

    var readline = require('readline');
    var rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });

    console.log('Visit the url: ', url);
    rl.question('Enter the code here:', getAccessToken);
  });
}

exports.prepareGoogleApi = prepareGoogleApi;