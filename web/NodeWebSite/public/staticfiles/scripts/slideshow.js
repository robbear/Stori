'use strict';

var slideShow = (function() {
    hFLog.log("In slideshow.js");

    var m_ssjUrl = null;
    var m_ssj = null;
    var m_currentSlideIndex = 0;
    var m_slideCount = 0;
    var m_imageControl = $('#slideImage');
    var m_prevButton = $('#prevButton');
    var m_nextButton = $('#nextButton');
    var m_orderArray = null;

    function _initializePage(ssjUrl) {
        m_ssjUrl = ssjUrl;

        // BUGBUG - remove
        $('#slideShareAddress').html(m_ssjUrl);

        m_prevButton.on('click', _onPrevButtonClicked);
        m_nextButton.on('click', _onNextButtonClicked);

        _fetchSlideShareJSON();
    }

    function _fetchSlideShareJSON() {
        $.getJSON(m_ssjUrl, _onFetchSlideShareJSONComplete);
    }

    function _loadImage(url) {
        m_imageControl.one('load', function(){
            _onImageControlLoadComplete();
        })
        .attr('src', url)
        .each(function() {
            // Cache fix for browsers that don't trigger .load()
            if (this.complete) {
                $(this).trigger('load');
            }
        });
    }

    function _onImageControlLoadComplete() {
        hFLog.log("_onImageControlLoadComplete");
    }

    function _onFetchSlideShareJSONComplete(json) {
        m_ssj = json;
        m_orderArray = m_ssj.order;
        m_slideCount = m_orderArray.length;

        var imageUrl = _getCurrentImageUrl();
        _loadImage(imageUrl);

        // BUGBUG - remove
        $('#ssj').html(JSON.stringify(m_ssj));
        $('#numSlides').html(m_slideCount);
    }

    function _getCurrentImageUrl() {
        var slideUuid = m_orderArray[m_currentSlideIndex];
        var sj = m_ssj.slides[slideUuid];

        return sj.image;
    }

    function _getCurrentAudioUrl() {
        var slideUuid = m_orderArray[m_currentSlideIndex];
        var sj = m_ssj.slides[slideUuid];

        return sj.audio;
    }

    function _onPrevButtonClicked() {
        if (m_currentSlideIndex <= 0) {
            m_currentSlideIndex = 0;
            return;
        }

        m_currentSlideIndex--;
        var url = _getCurrentImageUrl();
        _loadImage(url);
    }

    function _onNextButtonClicked() {
        if (m_currentSlideIndex >= m_slideCount - 1) {
            m_currentSlideIndex = m_slideCount - 1;
            return;
        }

        m_currentSlideIndex++;
        var url = _getCurrentImageUrl();
        _loadImage(url);
    }

    // Public methods
    return {
        initializePage: function(ssjUrl) {
            _initializePage(ssjUrl);
        },
        setSSJUrl: function(url) {
            m_ssjUrl = url;
        },
        getSSJUrl: function() {
            return m_ssjUrl;
        },
        getSSJ: function() {
            return m_ssj;
        }
    }
})();
