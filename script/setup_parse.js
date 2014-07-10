var fs = require('fs');

var secret = JSON.parse(fs.readFileSync('./parse_secret.json'))

function replace_secrets(filename) {
    var content = fs.readFileSync(filename).toString();
    content = content.replace(/APP_ID_PLACEHOLDER/, secret.app_id)
    content = content.replace(/CLIENT_KEY_PLACEHOLDER/, secret.client_key);
    content = content.replace(/STORAGE_SERVER_URL_PLACEHOLDER/, secret.storage_server_url);
    fs.writeFileSync(filename, content);
}

function replace_iOS() {
    replace_secrets('iOS/messtin-iOS/parse_secret.plist');
}

// for Android
function replace_android() {
    replace_secrets('android/app/src/main/res/values/parse_secret.xml');
}

replace_iOS();
replace_android();
