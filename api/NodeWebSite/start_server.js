var _databaseName = null;

exports.SetDatabaseName = function(dbName) {
    _databaseName = dbName;
};

exports.StartServer = function(startserver_callback, dbconnected_callback) {
    var fs = require('fs'),
        express = require('express'),
        app = express(),
        http = require('http'),
        https = require('https'),
        bodyParser = require('body-parser'),
        logger = require('./logger/logger'),
        pinger = require('./utilities/pinger'),
        config = require('./config/config.js');

    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({extended: true}));

    var router = express.Router();
    router.get('/', function(req, res) {
        res.json({message: 'Welcome to the Stori API'});
    });

    logger.bunyanLogger().info("%s***** Starting StoriAPI web service *****", config.TAG);
    if (_databaseName) config.setDatabaseName(_databaseName);

    //
    // HTTP server options
    //
    var http_options = {
        name: "StoriAPI HTTP",
        log: logger.bunyanLogger()
    };

    //
    // HTTPS server options
    //
    var https_options = config.usesHttps ? {
        name: "StoriAPI HTTPS",
        key: fs.readFileSync('./certificates/ssl/self-signed/server.key'),
        certificate: fs.readFileSync('./certificates/ssl/self-signed/server.crt'),
        log: logger.bunyanLogger()
    } : null;

    /* SOON
    var privateKey  = fs.readFileSync('sslcert/server.key', 'utf8');
    var certificate = fs.readFileSync('sslcert/server.crt', 'utf8');
    var credentials = {key: privateKey, cert: certificate};
    */

    // Instantiate the HTTP and HTTPS servers
    var httpServer = http.createServer(app);
    var httpsServer = config.usesHttps ? https.createServer(/*credentials*/null, app) : null;

    // Error handlers
    // BUGBUG - These should be updated to handle graceful shutdown of cluster
    // per http://nodejs.org/api/domain.html.
    httpServer.on('uncaughtException', function(req, res, route, err) {
        logger.bunyanLogger().error("%s*****Uncaught Exception*****: %s on route %s", config.TAG, err.message, route);
        if (res._headerSent) {
            return false;
        }

        res.send(new restify.InternalError("unexpected error"));
        return true;
    });

    if (config.usesHttps) {
        httpsServer.on('uncaughtException', function(req, res, route, err) {
            logger.bunyanLogger().error("%s*****Uncaught Exception*****: %s on route %s", config.TAG, err.message, route);
            if (res._headerSent) {
                return false;
            }

            res.send(new restify.InternalError("unexpected error"));
            return true;
        });
    }

    //
    // Pre-router callback.
    //
    function preRoutingHandler(req, res, next) {
        return next();
    }

    /* NEVER
    // Check userAgent for curl. If it is, this sets the Connection header
    // to "close" and removes the "Content-Length" header.
    httpServer.pre([restify.pre.userAgentConnection(), preRoutingHandler]);

    if (config.usesHttps) {
        httpsServer.pre([restify.pre.userAgentConnection(), preRoutingHandler]);
    }
    */

    //
    // Routes - see routes/router.js
    //
    /*
    for (var i = 0; i < router.routeMap.length; i++) {
        var route = router.routeMap[i];

        server[route.httpVerb](route.route, route.serverHandler);
        if (config.usesHttps) {
            https_server[route.httpVerb](route.route, route.serverHandler);
        }
    }
    */
    app.use('/api', router);

    //
    // Connect to the database
    //

    var isStartupConnectionAttempt = true;

    function onDatabaseConnect(err) {
        if (err && isStartupConnectionAttempt) {
            // Re-attempt only if this is part of app startup. Otherwise, we rely on auto-reconnect in
            // the mongodb native driver to try again.
            logger.bunyanLogger().error('%sFailed to connect to MongoDB. Attempting to connect again. Err: %s', config.TAG, err.message);
            setTimeout(connectWithRetry, 5000);
            return;
        }
        else {
            isStartupConnectionAttempt = false;
            logger.bunyanLogger().info('%sMongoDB connection established', config.TAG);
            if (dbconnected_callback) {
                dbconnected_callback();
            }

            pinger.start();
        }
    }

    function onDatabaseReconnect() {
        logger.bunyanLogger().info('%sSuccessfully reconnected to MongoDB', config.TAG);
    }

    function onDatabaseDisconnect(err) {
        if (isStartupConnectionAttempt) return;

        logger.bunyanLogger().error('%sLost connection to MongoDB. Err: %s', config.TAG, err.message);
    }

    var connectWithRetry = function() {
        logger.bunyanLogger().info('%sAttempting to connect to MongoDB. isStartupConnectionAttempt = %s', config.TAG, isStartupConnectionAttempt);
        var connectionString = config.connectionString;
        return config.dbUtils.connectToMongoDB(connectionString, config.databaseOptions, onDatabaseConnect, null, onDatabaseDisconnect, onDatabaseReconnect);
    };

    logger.bunyanLogger().info("%s*** Calling connectWithRetry", config.TAG);
    connectWithRetry();

    //
    // Start the servers on the appropriate ports
    //
    var port = process.env.PORT || config.httpPort;
    httpServer.listen(port, function() {
        logger.bunyanLogger().info('%s listening at port %s in %s mode', config.TAG, port, config.environment);
        logger.bunyanLogger().info("Using node.js %s", process.version);

        if (config.usesHttps) {
            https_server.listen(config.httpsPort, function() {
                logger.bunyanLogger().info('%s listening at port %s', config.TAG,  port);

                if (startserver_callback) {
                    startserver_callback();
                }
            });
        }
        else {
            if (startserver_callback) {
                startserver_callback();
            }
        }
    });
};
