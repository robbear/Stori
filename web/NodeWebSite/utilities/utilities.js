var logger = require('../logger/logger'),
    hfConfig = require('../utilities/config'),
    fs = require('fs'),
    hogan = require('hogan.js');

exports.timestampString = "uninitialized";
exports.versionString = "staticfiles";
exports.appTemplates = {};
exports.cacheSeconds = 60 * 15; // 15 minute cache expiration

exports.initTemplates = function() {
    fs.readFile('views/layout.html', 'utf-8', function(err, layoutString) {
        exports.appTemplates.wrapper = hogan.compile(layoutString.toString('utf-8'));
    });

    fs.readFile('views/slides_layout.html', 'utf-8', function(err, layoutString) {
        exports.appTemplates.slides_wrapper = hogan.compile(layoutString.toString('utf-8'));
    });
};

exports.sendResponse = function(res, data) {
    res.send(data);
    logger.bunyanLogger().info({res: res}, "RESPONSE");
};


exports.sendOutputHtml = function(master, req, res, headerContentPath, bodyContentPath, propertyBag) {
    propertyBag.EnableClientLogging = hfConfig.isClientLoggingEnabled;
    propertyBag.StaticFiles = exports.versionString;

    var templateWrapper;
    if (!master || master == "root") {
        templateWrapper = exports.appTemplates.wrapper;
    }
    else if (master == "slides") {
        templateWrapper = exports.appTemplates.slides_wrapper;
    }

    res.setHeader('Cache-Control', 'public,max-age=' + exports.cacheSeconds);
    res.setHeader('ETag', exports.versionString);

    var fnReadBody = function (bodyContentPath, headerContentString) {
        fs.readFile(bodyContentPath, 'utf-8', function(err, bodyContentString) {
            var partials = { "HeaderContent": headerContentString, "BodyContent": bodyContentString };
            var outputHtml = templateWrapper.render(propertyBag, partials);

            res.send(outputHtml);
        });
    };

    var fnReadHeader = function (headerContentPath, bodyContentPath) {
        fs.readFile(headerContentPath, 'utf-8', function(err, headerContentString) {
            fnReadBody(bodyContentPath, headerContentString);
        });
    };

    // Define this helper function to be used as a callback in the alternate if/else
    // code paths below. Remember that everything needs to stay async.
    var fnSendOutput = function(headerContentPath, bodyContentPath) {
        fnReadHeader(headerContentPath, bodyContentPath);
    };

    fnSendOutput(headerContentPath, bodyContentPath);
};

// Read in Timestamp file
fs.readFile('timestamp.txt', 'utf-8', function (err, ts) {
    if (ts) {
        exports.timestampString = ts.trim();
    }
});

// Read in version string in version.txt
fs.readFile('version.txt', 'utf-8', function(err, vs) {
    if (vs) {
        exports.versionString = vs.trim();
    }
});

// Read in package.json for module versions
fs.readFile('package.json', 'utf-8', function(err, package) {
    if (package) {
        exports.package_json = JSON.parse(package);
    }
})


