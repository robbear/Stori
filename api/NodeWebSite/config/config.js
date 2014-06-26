
var logger = require('../logger/logger');

var _env = process.env.NODE_ENV || 'development';

//
// Configuration
//
var _modelsPath;
var _mongoBaseUrl;
var _databaseName = 'stori';
var _usesHttps = false;
var _TAG = "STORI_API: ";
var _pingerTimeoutSeconds = 45;
var _usePinger = true;

if ('development' === _env) {
    _modelsPath = '../../../models/';
    _mongoBaseUrl = 'mongodb://localhost:27017/';
}
if ('production' === _env) {
    _modelsPath = '../models/';
    _mongoBaseUrl = 'mongodb://hfmongo:password.mongolab.com:53300/';
}

//
// See https://groups.google.com/forum/?fromgroups=#!topic/mongoose-orm/0bOPcbCD12Q for
// information regarding keep-alive
//
exports.databaseOptions = {
    /*
    replset: {
        strategy: 'ping',
        rs_name: 'somerepsetname',
        readSecondary: true,
        socketOptions: {
            keepAlive: 1
        }
    },
    */
    server: {
        poolSize: 10,
        auto_reconnect: true,
        socketOptions: {
            keepAlive: 1
        }
    }
};

exports.httpPort = 1338;
exports.httpsPort = 443;
exports.usesHttps = _usesHttps;
exports.connectionString = _mongoBaseUrl + _databaseName;
exports.dbUtils = require(_modelsPath + 'dbutils.js');
exports.userModel = require(_modelsPath + 'user.js');
// Other models...
exports.TAG = _TAG;
exports.usePinger = _usePinger;
exports.pingerTimeoutSeconds = _pingerTimeoutSeconds;
exports.environment = _env;

exports.useLogging = function(useLogging) {
    _logger.useLogging(useLogging);
}

exports.setPinger = function(usePinger, seconds) {
    _usePinger = usePinger;
    _pingerTimeoutSeconds = seconds;
}

//
// Primarily used for unit tests to set the database name
//
exports.setDatabaseName = function(dbName) {
    _databaseName = dbName;
}
