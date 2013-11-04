'use strict';

var slideShow = (function() {
    console.log("slideshow.js");

    var m_ssjUrl = null;
    var m_ssj = null;

    function _fetchSlideShareJSON() {
        $.getJSON(m_ssjUrl, function(json) {
            m_ssj = json;
            $('#ssj').html(JSON.stringify(m_ssj));
            $('#numSlides').html(m_ssj["order"].length);
        });
    }

    // Public methods
    return {
        initializePage: function() {
            $('#slideShareAddress').html(m_ssjUrl);
        },
        setSSJUrl: function(url) {
            m_ssjUrl = url;
        },
        getSSJUrl: function() {
            return m_ssjUrl;
        },
        getSSJ: function() {
            return m_ssj;
        },
        fetchSlideShareJSON: _fetchSlideShareJSON
    }
})();
