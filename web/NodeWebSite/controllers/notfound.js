
var express = require('express'),
    utilities = require('../utilities/utilities');

var app = module.exports = express();

app.get('/notfound', notFound);
app.handler = notFound;

function notFound(req, res) {
    res.status(404);
    if (req.accepts('html')) {
        utilities.sendOutputHtml("root", req, res, 'views/404_header.html', 'views/404_body.html',
            {
                "PageTitle": "Not found"
            });
        return;
    }

    if (req.accepts('json')) {
        res.send({ error: 'Not found' });
        return;
    }

    res.type('txt');
    utilities.sendResponse(res, "Not found");
}
