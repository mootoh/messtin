Messtin
=======

Android/iOS app for reading scanned books.

Setup
-----

- `brew install ghoastscript jq`
- Setup an http server which hosts book image data
- Create a [parse.com](http://parse.com) application
- Edit script/secret.json to have parameters

Then Upload a PDF file as a sequence of jpg files:

    % cd script
    % sh upload_to_server.sh <PDF_FILE> <TITLE_OF_THE_BOOK>

### Parameters

- "app\_id": Parse.com app id
- "client\_key": Parse.com client id
- "storage\_server": HTTP server base url (ex: http://localhost:8000)

----

![messkit](http://upload.wikimedia.org/wikipedia/commons/6/6b/Japanese_Messkit_CylinderType_2.JPG)
