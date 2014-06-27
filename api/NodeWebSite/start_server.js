var _databaseName = null;

exports.SetDatabaseName = function(dbName) {
    _databaseName = dbName;
};

exports.StartServer = function(startserver_callback, dbconnected_callback) {
    var fs = require('fs'),
        Hapi = require('hapi'),
        logger = require('./logger/logger'),
        pinger = require('./utilities/pinger'),
        config = require('./config/config.js');

    //
    // Controllers
    //
    var defaultController = require('./controllers/default');
    var testController = require('./controllers/test');


    var port = +process.env.PORT || +config.httpPort;

    /* SOON
     var privateKey  = fs.readFileSync('sslcert/server.key', 'utf8');
     var certificate = fs.readFileSync('sslcert/server.crt', 'utf8');
     var credentials = {key: privateKey, cert: certificate};
     */
    var credentials = {};
    var httpServerOptions = {
                                tls: config.usesHttps ? credentials : null
                            };
    var httpServer = Hapi.createServer('0.0.0.0', port, httpServerOptions);

    httpServer.app.logger = logger.bunyanLogger();

    logger.bunyanLogger().info("%s***** Starting StoriAPI web service *****", config.TAG);
    if (_databaseName) config.setDatabaseName(_databaseName);

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

    var routes = [
        { path: config.apiRoot, method: 'GET', config: defaultController.config },
        { path: config.apiRoot + '/test', method: 'GET', config: testController.config }
    ];

    httpServer.route(routes);

    httpServer.start(function(err) {
        if (err) {
            logger.bunyanLogger().info('%s Server failed to start: %s', err);
            return;
        }

        logger.bunyanLogger().info('%s listening at port %s in %s mode', config.TAG, port, config.environment);
        logger.bunyanLogger().info("Using node.js %s", process.version);

        if (startserver_callback) {
            startserver_callback();
        }
    });
};
