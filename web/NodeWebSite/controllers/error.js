
var express = require('express'),
    utilities = require('../utilities/utilities');

var app = module.exports = express();

app.get('/error', errorFunction);
app.handler = errorFunction;

function errorFunction(req, res) {
    res.status(500);
    if (req.accepts('html')) {
        utilities.sendOutputHtml("root", req, res, 'views/500_header.html', 'views/500_body.html',
            {
                "PageTitle": "Error"
            });
        return;
    }

    if (req.accepts('json')) {
        res.send({ error: 'Error' });
        return;
    }

    res.type('txt');
    utilities.sendResponse(res, "Error");
};