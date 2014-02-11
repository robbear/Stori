'use strict';

var hfUtilities = (function() {

    var m_versionString;

    return {
        setVersionString: function(s) {
            m_versionString = s;
        },
        getVersionString: function() {
            return m_versionString;
        },
        getInternalImagePath: function(imageFile) {
            return "/" + this.getVersionString() + "/images/" + imageFile;
        }
    }
})();