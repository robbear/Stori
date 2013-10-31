var _env = process.env.NODE_ENV || 'development';

var _isClientLoggingEnabled = false;    // Set to false for production. If true, console.log is used.
var _logStaticResources = false;        // Set to false for production. If true, logs all HTTP requests

if ('development' === _env) {
    _isClientLoggingEnabled = false;
    _logStaticResources = false;
}
if ('production' === _env) {
    _isClientLoggingEnabled = false;
    _logStaticResources = false;
}

exports.environment = _env;
exports.isClientLoggingEnabled = _isClientLoggingEnabled;
exports.logStaticResources = _logStaticResources;
