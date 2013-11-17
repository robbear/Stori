'use strict';

var slideShow = (function() {
    hFLog.log("In slideshow.js");

    var m_ssjUrl = null;
    var m_ssj = null;
    var m_currentSlideIndex = 0;
    var m_slideCount = 0;
    var m_slidesjsDiv = $('#slides');
    var m_jPlayerDiv = $('#jquery_jplayer_1');
    var m_isPlayerConstructed = false;
    var m_orderArray = null;
    var m_audioPlaying = false;

    function _initializePage(ssjUrl) {
        m_ssjUrl = ssjUrl;

        m_slidesjsDiv.on('click', _onImageClicked);

        // Test for audio support
        if (Modernizr.audio.mp3) {
            $('#dialog-not-supported-text').hide();
        }
        else {
            $('#dialog-welcome-text').hide();
        }
        $("#opening-dialog").dialog({
            modal: true,
            buttons: {
                Ok: function() {
                    $(this).dialog("close");
                    var audioUrl = _getCurrentAudioUrl();
                    _playAudio(audioUrl);
                }
            }
        });

        _fetchSlideShareJSON();
    }

    function _fetchSlideShareJSON() {
        $.getJSON(m_ssjUrl, _onFetchSlideShareJSONComplete);
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
                    m_audioPlaying = true;
                },
                ended: function() {
                    hFLog.log("_playAudio.jPlayer has stopped playing");
                    m_audioPlaying = false;
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
            m_audioPlaying = true;
        }
    }

    function _prefetchAudio() {
        if (m_ssj == null) return;

        for (var i = 0; i < m_slideCount; i++) {
            var url = _getAudioUrl(i);
            $.ajax({url: url, success: function() {
                hFLog.log("Prefetched " + url);
            }});
        }
    }

    function _onFetchSlideShareJSONComplete(json) {
        m_ssj = json;
        m_orderArray = m_ssj.order;
        m_slideCount = m_orderArray.length;

        _prefetchAudio();

        var html = "";
        for (var i = 0; i < m_slideCount; i++) {
            var url = _getImageUrl(i);
            html += '<img src="' + url + '"/>'
        }

        m_slidesjsDiv.html(html);
        hFLog.log("Initializing slidesjs");
        m_slidesjsDiv.slidesjs({
            width: 100,
            height: 200,
            callback: {
                loaded: function(number) {
                    hFLog.log("slides.loaded: number=" + number);
                    $('.slidesjs-next').html("");
                    $('.slidesjs-previous').html("");

                    m_currentSlideIndex = number - 1;

                    // Hide pagination
                    $('.slidesjs-navigation, .slidesjs-pagination').hide(0);
                },
                start: function(number) {
                    hFLog.log("slides.start: number=" + number);
                },
                complete: function(number) {
                    hFLog.log("slides.complete: number=" + number);
                    m_currentSlideIndex = number - 1;

                    var audioUrl = _getCurrentAudioUrl();
                    hFLog.log("audioUrl = " + audioUrl);
                    if (audioUrl) {
                        _playAudio(audioUrl);
                    }
                }
            }
        });
    }

    function _getImageUrl(index) {
        var slideUuid = m_orderArray[index];
        var sj = m_ssj.slides[slideUuid];

        return sj.image;
    }

    function _getAudioUrl(index) {
        var slideUuid = m_orderArray[index];
        var sj = m_ssj.slides[slideUuid];

        return sj.audio;
    }

    function _getCurrentImageUrl() {
        return _getImageUrl(m_currentSlideIndex);
    }

    function _getCurrentAudioUrl() {
        var slideUuid = m_orderArray[m_currentSlideIndex];
        var sj = m_ssj.slides[slideUuid];

        return sj.audio;
    }

    function _onImageClicked(e) {
        e.preventDefault();

        hFLog.log("_onImageClicked");

        var width = m_slidesjsDiv.width();
        var clickX = e.offsetX;
        hFLog.log("_onImageClicked: width=" + width + " and clickX=" + clickX);

        if (clickX < (width / 3)) {
            hFLog.log("_onImageClicked: left click - previous");
            m_slidesjsDiv.slidesjs.previous();
        }
        else if (clickX > ((2 * width) / 3)) {
            hFLog.log("_onImageClicked: right click - next");
            m_slidesjsDiv.slidesjs.next();
        }
        else {
            // Toggle audio
            if (m_audioPlaying) {
                hFLog.log("_onImageClicked: middle click, m_audioPlaying==true, so calling jPlayer(stop)");
                m_jPlayerDiv.jPlayer("stop");
                m_audioPlaying = false;
            }
            else {
                hFLog.log("_onImageClicked: middle click, m_audioPlaying==false, so calling _playAudio");
                var url = _getCurrentAudioUrl();
                _playAudio(url);
                m_audioPlaying = true;
            }
        }

        return false;
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
