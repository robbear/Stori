var config = require('../config/config'),
    logger = require('../logger/logger');

exports.config = {
    handler: handleRequest
};

function handleRequest(req, reply) {
    logger.bunyanLogger().info('%sGET %s/test', config.TAG, config.apiRoot);
    reply({ message: 'Test API called' });
}
