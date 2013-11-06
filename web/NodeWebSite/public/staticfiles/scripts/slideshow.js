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
    var m_startButton = $('#startButton');
    var m_jPlayerDiv = $('#jquery_jplayer_1');
    var m_isPlayerConstructed = false;
    var m_orderArray = null;
    var m_showStarted = false;

    function _initializePage(ssjUrl) {
        m_ssjUrl = ssjUrl;

        m_prevButton.on('click', _onPrevButtonClicked);
        m_nextButton.on('click', _onNextButtonClicked);
        m_startButton.on('click', _onStartButtonClicked);

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

    function _playAudio(url) {
        hFLog.log("_playAudio: " + url);

        if (!m_isPlayerConstructed) {
            hFLog.log("Constructing player");
            m_jPlayerDiv.jPlayer({
                ready: function() {
                    hFLog.log("_playAudio.jPlayer.ready");
                    m_isPlayerConstructed = true;
                    $(this).jPlayer("setMedia", { mp3: url });
                    $(this).jPlayer("play");
                },
                supplied: "mp3",
                swfPath: "/" + hfUtilities.getVersionString() + "/lib/jQuery.jPlayer.2.4.0/Jplayer.swf",
                solution: "html, flash",
                errorAlerts: true,
                warningAlerts: false
            });
        }
        else {
            hFLog.log("Player constructed - setting the media and playing");
            m_jPlayerDiv.jPlayer("setMedia", { mp3: url });
            m_jPlayerDiv.jPlayer("play");
        }
    }

    function _onImageControlLoadComplete() {
        hFLog.log("_onImageControlLoadComplete");

        var audioUrl = _getCurrentAudioUrl();
        hFLog.log("audioUrl = " + audioUrl);
        if (audioUrl && m_showStarted) {
            _playAudio(audioUrl);
        }
    }

    function _onFetchSlideShareJSONComplete(json) {
        m_ssj = json;
        m_orderArray = m_ssj.order;
        m_slideCount = m_orderArray.length;

        var imageUrl = _getCurrentImageUrl();
        _loadImage(imageUrl);
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

    function _onStartButtonClicked() {
        m_showStarted = true;

        m_startButton.addClass("hidden");
        m_prevButton.removeClass("hidden");
        m_nextButton.removeClass("hidden");

        var url = _getCurrentAudioUrl();
        if (url) {
            _playAudio(url);
        }
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
