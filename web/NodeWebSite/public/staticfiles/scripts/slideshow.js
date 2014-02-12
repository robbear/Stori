'use strict';

var slideShow = (function() {
    hFLog.log("In slideshow.js");

    var m_ssjUrl = null;
    var m_ssj = null;
    var m_currentSlideIndex = 0;
    var m_slideCount = 0;
    var m_slidesjsDiv = $('#slides');
    var m_slideTextControl = $('#slidetext');
    var m_playStopControl = $('#playstopcontrol');
    var m_overlay = $('#overlay');
    var m_slidePositionControl = $('#slideposition');
    var m_slideTitle = $('#slidetitle');
    var m_jPlayerDiv = $('#jquery_jplayer_1');
    var m_isPlayerConstructed = false;
    var m_orderArray = null;
    var m_audioPlaying = false;
    var m_supportsTouch = false;
    var m_isInternetExplorer = false;
    var m_navControlClicked = false;

    // Remember: m_nextControl and m_prevControl aren't instantiated until the sliderjs control is loaded
    var m_nextControl;
    var m_prevControl;


    function isInternetExplorer() {
        var index = navigator.userAgent.indexOf("Trident");
        return (index >= 0);
    }

    function supportsTouch() {
        return Modernizr.touch;
    }

    function _getSlidePositionText() {
        return (m_currentSlideIndex + 1) + " of " + m_slideCount;
    }

    function _initializePage(ssjUrl) {
        m_supportsTouch = supportsTouch();
        hFLog.log("m_supportsTouch is " + m_supportsTouch);

        m_isInternetExplorer = isInternetExplorer();
        hFLog.log("m_isInternetExplorer is " + m_isInternetExplorer);

        m_ssjUrl = ssjUrl;

        m_slidesjsDiv.on('click', _onImageClicked);
        m_playStopControl.on('click', _onPlayStopClicked);

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
                    if (audioUrl) {
                        _playAudio(audioUrl);
                    }
                }
            }
        });

        _fetchSlideShareJSON();
    }

    function _togglePlayStopControl() {
        m_playStopControl.attr('src', hfUtilities.getInternalImagePath(m_audioPlaying ? "ic_stopplaying.png" : "ic_play.png"));
    }

    function _toggleOverlay() {
        hFLog.log("_toggleOverlay");

        m_slideTextControl.toggle();
        m_nextControl.toggle();
        m_prevControl.toggle();
        m_playStopControl.toggle();
        m_slidePositionControl.toggle();
        m_slideTitle.toggle();
        m_overlay.toggle();
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
                    _togglePlayStopControl();
                },
                ended: function() {
                    hFLog.log("_playAudio.jPlayer has stopped playing");
                    m_audioPlaying = false;
                    _togglePlayStopControl();
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
            _togglePlayStopControl();
        }
    }

    function _prefetchAudio() {
        if (m_ssj == null) return;

        for (var i = 0; i < m_slideCount - 1; i++) {
            var url = _getAudioUrl(i);
            $.ajax({url: url, success: function() {
                hFLog.log("Prefetched " + url);
            }});
        }

        // BUGBUG: TODO: prefetch the upsell audio, too.
    }

    function _onFetchSlideShareJSONComplete(json) {
        m_ssj = json;
        m_orderArray = m_ssj.order;
        m_slideCount = m_orderArray.length + 1;

        // User-supplied string. HTML encode it via call to text()
        m_slideTitle.text(m_ssj.title);
        m_slidePositionControl.text(_getSlidePositionText());
        m_slideTextControl.html(_getHtmlSafeCurrentSlideText());

        _prefetchAudio();

        var html = "";
        for (var i = 0; i < m_slideCount; i++) {
            var url = _getImageUrl(i);
            html += '<img src="' + url + '"/>';
        }

        m_slidesjsDiv.html(html);
        hFLog.log("Initializing slidesjs");
        m_slidesjsDiv.slidesjs({
            width: 100,
            height: 200,
            callback: {
                loaded: function(number) {
                    hFLog.log("slides.loaded: number=" + number);

                    // Remember: the next/prev controls aren't instantiated until here.
                    m_nextControl = $('#slides .slidesjs-next');
                    m_prevControl = $('#slides .slidesjs-previous');

                    m_nextControl.on('click', function() {
                        hFLog.log("**** Got click on next control");
                        m_navControlClicked = true;
                    });
                    m_prevControl.on('click', function() {
                        hFLog.log("**** Got click on prev control");
                        m_navControlClicked = true;
                    });

                    m_nextControl.html("<img src='" + hfUtilities.getInternalImagePath("ic_arrow_right.png") + "'/>");
                    m_prevControl.html("<img src='" + hfUtilities.getInternalImagePath("ic_arrow_left.png") + "'/>");

                    m_currentSlideIndex = number - 1;

                    $('.slidesjs-pagination').hide(0);
                },
                start: function(number) {
                    hFLog.log("slides.start: number=" + number);
                },
                complete: function(number) {
                    hFLog.log("slides.complete: number=" + number);
                    m_currentSlideIndex = number - 1;
                    m_slidePositionControl.text(_getSlidePositionText());
                    m_slideTextControl.html(_getHtmlSafeCurrentSlideText());

                    var audioUrl = _getCurrentAudioUrl();
                    hFLog.log("audioUrl = " + audioUrl);
                    if (audioUrl) {
                        _playAudio(audioUrl);
                    }
                    else if (m_audioPlaying) {
                        _onPlayStopClicked();
                    }
                }
            }
        });
    }

    function _getImageUrl(index) {
        if (index == (m_slideCount - 1)) {
            return "/" + hfUtilities.getVersionString() + "/images/lastslide.png";
        }

        var slideUuid = m_orderArray[index];
        var sj = m_ssj.slides[slideUuid];

        return sj.image;
    }

    function _getAudioUrl(index) {
        if (index == (m_slideCount - 1)) {
            // BUGBUG TODO: need upsell audio
            return null;
        }

        var slideUuid = m_orderArray[index];
        var sj = m_ssj.slides[slideUuid];

        return sj.audio;
    }

    //
    // Returns an HTML encoded string with linkable URLs
    //
    function _getHtmlSafeSlideText(index) {
        if (index == (m_slideCount - 1)) {
            return "Get a better viewing experience. Download the Stori application now!";
        }

        var slideUuid = m_orderArray[index];
        var sj = m_ssj.slides[slideUuid];

        if (sj.text == null) {
            return null;
        }

        // HTML encode the slide text
        var slideText = $('<div/>').text(sj.text).html();

        // Decoration function for any URLs embedded in the slide text
        var decorate = function(url) {
            var uri = new URI(url);
            if (!uri.protocol()) {
                uri = uri.protocol("http");
            }
            uri = uri.normalize();

            return "<a href=\"" + uri + "\" target=\"_blank\">" + url + "</a>";
        }

        // Call URI.withinString to replace embedded URLs with anchor links.
        // Note: our start expression drops the requirement for terminating '/' character,
        // allowing "foo.com" rather than "foo.com/"
        // To add it back:
        // start: /\b(?:([a-z][a-z0-9.+-]*:\/\/)|www\.|[a-z]+\.[a-z]{2,4}\/)/gi
        var linkedString = URI.withinString(slideText, decorate, {
            start: /\b(?:([a-z][a-z0-9.+-]*:\/\/)|www\.|[a-z]+\.[a-z]{2,4})/gi
        });

        hFLog.log("_getHtmlSafeSlideText returns:");
        hFLog.log(linkedString);

        return linkedString;
    }

    function _getCurrentImageUrl() {
        return _getImageUrl(m_currentSlideIndex);
    }

    function _getCurrentAudioUrl() {
        return _getAudioUrl(m_currentSlideIndex);
    }

    function _getHtmlSafeCurrentSlideText() {
        return _getHtmlSafeSlideText(m_currentSlideIndex);
    }

    function _onPlayStopClicked(e) {
        if (e) {
            e.preventDefault();
        }

        hFLog.log("_onPlayStopClicked");

        // Toggle audio
        if (m_audioPlaying) {
            hFLog.log("_onPlayStopClicked: m_audioPlaying==true, so calling jPlayer(stop)");
            m_jPlayerDiv.jPlayer("stop");
            m_audioPlaying = false;
            _togglePlayStopControl();
        }
        else {
            hFLog.log("_onPlayStopClicked: m_audioPlaying==false, so calling _playAudio");
            var url = _getCurrentAudioUrl();
            if (url) {
                _playAudio(url);
                m_audioPlaying = true;
            }
        }

        return false;
    }

    function _onImageClicked(e) {
        e.preventDefault();

        if (m_navControlClicked) {
            hFLog.log("_onImageClicked - ignoring since m_navControClicked is true.");
            m_navControlClicked = false;
            return false;
        }

        hFLog.log("_onImageClicked");
        hFLog.log(e);

        _toggleOverlay();

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
