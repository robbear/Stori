var bunyan = require('bunyan');

var _useLogging = true;

var _stubLogger = {
    info: function() {},
    trace: function() {},
    error: function() {}
};

var _bunyanLogger = bunyan.createLogger({
    name: 'storiapi',
    streams: [
        {
            level: 'info',
            path: './logfiles/storiapi-info.log',
            type: 'rotating-file',
            period: '1d',
            count: 10
        },
        {
            level: 'error',
            path: './logfiles/storiapi-errors.log',
            type: 'rotating-file',
            period: '1d',
            count: 10
        }
    ],
    serializers: {
        req: reqSerializer,
        res: resSerializer,
        err: bunyan.stdSerializers.err
    }
});

exports.useLogging = function(useLogging) {
    _useLogging = useLogging;
}

exports.bunyanLogger = function () {
    return _bunyanLogger;
}

function reqSerializer(req) {
    if (!req || !req.connection)
        return req;
    return {
        req_id: req.req_id,
        method: req.method,
        url: req.url,
        headers: req.headers,
        remoteAddress: req.connection.remoteAddress,
        remotePort: req.connection.remotePort
    };
}

function resSerializer(res) {
    if (!res || !res.statusCode)
        return res;
    return {
        req_id: res.req.req_id,
        statusCode: res.statusCode,
        header: res._header
    }
}
