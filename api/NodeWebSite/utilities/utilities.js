var logger = require('../logger/logger');

exports.isErrorDatabaseDisconnect = function(err) {
    if (!err || !err.message) return false;

    return err.message.indexOf("failed to connect to") == 0;
}
