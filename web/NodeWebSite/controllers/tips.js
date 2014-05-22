
var express = require('express'),
    utilities = require('../utilities/utilities');

var app = module.exports = express();

app.get('/tips', function(req, res) {
    utilities.sendOutputHtml("root", req, res, 'views/tips_header.html', 'views/tips_body.html',
        {
            "PageTitle": "Tips - Stori",
            "VersionString": utilities.versionString
        });
});
