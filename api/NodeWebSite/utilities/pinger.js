var logger = require('../logger/logger'),
    config = require('../config/config.js');

var User = config.userModel;

function ping() {
    logger.bunyanLogger().info("%s ***** PING *****", config.TAG);
    User.count({}, function(err, count) {
        logger.bunyanLogger().info("%s ***** PING RESULT: %s *****", config.TAG, err ? err.message : "ok");

        if (config.usePinger) {
            setTimeout(ping, config.pingerTimeoutSeconds * 1000);
        }
    });
}

exports.start = function() {
    if (!config.usePinger) {
        return;
    }

    setTimeout(ping, config.pingerTimeoutSeconds * 1000);
};
