'use strict';

var hFLog = (function () {

	var isLoggingOn = false;

	return {
		enableLogging: function (enable) {
			isLoggingOn = enable;
			return isLoggingOn;
		},

		log: function (message) {
			if (!isLoggingOn) {
				return;
			}

			if (typeof console === "object") {
				console.log(message);
			}
		}
	}
})();
