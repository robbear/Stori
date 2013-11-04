'use strict';

var hFLog = (function () {

	var m_isLoggingOn = true;

	return {
		enableLogging: function (enable) {
			m_isLoggingOn = enable;
			return m_isLoggingOn;
		},

		log: function (message) {
			if (!m_isLoggingOn) {
				return;
			}

			if (typeof console === "object") {
				console.log(message);
			}
		}
	}
})();
