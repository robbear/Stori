
var express = require('express'),
    utilities = require('../utilities/utilities');

var app = module.exports = express();

app.get('/', function(req, res) {
    utilities.sendOutputHtml("root", req, res, 'views/index_header.html', 'views/index_body.html',
        {
            "PageTitle": "neodori",
            "VersionString": utilities.versionString
        });
});