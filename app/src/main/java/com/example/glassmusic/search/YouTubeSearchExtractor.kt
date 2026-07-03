package com.example.glassmusic.search

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.glassmusic.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray

class YouTubeSearchExtractor(context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null

    val resultsFlow = MutableStateFlow<List<Track>>(emptyList())
    val isLoadingFlow = MutableStateFlow(false)

    init {
        // WebView instances must be created on the Main (UI) thread on Android
        mainHandler.post {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                addJavascriptInterface(this@YouTubeSearchExtractor, "AndroidSearchBridge")
            }
        }
    }

    /**
     * Triggers search inside the headless WebView
     */
    fun search(query: String) {
        isLoadingFlow.value = true
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://m.youtube.com/results?search_query=$encodedQuery"
        
        mainHandler.post {
            webView?.let { view ->
                view.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView?, url: String?) {
                        super.onPageFinished(v, url)
                        if (v != null) {
                            injectExtractorJs(v)
                        }
                    }
                }
                view.loadUrl(url)
            }
        }
    }

    private fun injectExtractorJs(view: WebView) {
        val js = """
            (function() {
                setTimeout(function() {
                    var tracks = [];
                    
                    // Attempt 1: Parse from ytInitialData JSON object
                    if (window.ytInitialData) {
                        try {
                            var data = window.ytInitialData;
                            var items = null;
                            
                            if (data.contents && data.contents.sectionListRenderer) {
                                items = data.contents.sectionListRenderer.contents[0].itemSectionRenderer.contents;
                            } else if (data.contents && data.contents.twoColumnSearchResultsRenderer) {
                                items = data.contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents[0].itemSectionRenderer.contents;
                            }
                            
                            if (items) {
                                for (var i = 0; i < items.length; i++) {
                                    var item = items[i];
                                    if (item.videoRenderer) {
                                        var v = item.videoRenderer;
                                        var videoId = v.videoId;
                                        
                                        var isLive = false;
                                        if (v.badges) {
                                            var badgeText = JSON.stringify(v.badges).toUpperCase();
                                            if (badgeText.indexOf("LIVE") >= 0) isLive = true;
                                        }
                                        if (v.thumbnailOverlays) {
                                            var overlayText = JSON.stringify(v.thumbnailOverlays).toUpperCase();
                                            if (overlayText.indexOf("LIVE") >= 0) isLive = true;
                                        }
                                        if (!v.lengthText) {
                                            isLive = true;
                                        }
                                        
                                        var finalId = (isLive ? "ytlive_" : "") + videoId;
                                        
                                        var title = "Unknown Title";
                                        if (v.title && v.title.runs && v.title.runs.length > 0) {
                                            title = v.title.runs[0].text;
                                        } else if (v.title && v.title.simpleText) {
                                            title = v.title.simpleText;
                                        }
                                        
                                        var author = "Unknown Artist";
                                        if (v.ownerText && v.ownerText.runs && v.ownerText.runs.length > 0) {
                                            author = v.ownerText.runs[0].text;
                                        } else if (v.shortBylineText && v.shortBylineText.runs && v.shortBylineText.runs.length > 0) {
                                            author = v.shortBylineText.runs[0].text;
                                        }
                                        
                                        var artworkUrl = "";
                                        if (v.thumbnail && v.thumbnail.thumbnails && v.thumbnail.thumbnails.length > 0) {
                                            artworkUrl = v.thumbnail.thumbnails[0].url;
                                        }
                                        
                                        var durationText = v.lengthText ? (v.lengthText.runs ? v.lengthText.runs[0].text : v.lengthText.simpleText) : "0:00";
                                        if (isLive) {
                                            durationText = "LIVE";
                                        }
                                        
                                        tracks.push({
                                            id: finalId,
                                            title: title,
                                            artist: author,
                                            artworkUrl: artworkUrl,
                                            durationText: durationText
                                        });
                                    }
                                }
                            }
                        } catch(e) {}
                    }
                    
                    // Attempt 2: DOM Scraping fallback (if JSON failed or is missing)
                    if (tracks.length === 0) {
                        var elements = document.querySelectorAll('ytm-video-with-context-renderer, ytm-compact-video-renderer, ytm-video-renderer');
                        elements.forEach(function(el) {
                            try {
                                var linkEl = el.querySelector('a.media-item-thumbnail-container, a.compact-media-item-image, a');
                                var href = linkEl ? linkEl.getAttribute('href') : '';
                                var videoId = '';
                                if (href.indexOf('v=') >= 0) {
                                    videoId = href.split('v=')[1].split('&')[0];
                                } else if (href.indexOf('/watch/') >= 0) {
                                    videoId = href.split('/watch/')[1].split('?')[0];
                                }
                                
                                if (!videoId) return;
                                
                                var durationEl = el.querySelector('var.video-thumbnail-overlay-time-status, span.video-duration, ytm-thumbnail-overlay-time-status-renderer');
                                var durationText = durationEl ? durationEl.innerText.trim() : '0:00';
                                
                                var isLive = false;
                                if (durationText.toUpperCase().indexOf("LIVE") >= 0 || !durationEl || el.innerText.toUpperCase().indexOf("LIVE") >= 0) {
                                    isLive = true;
                                    durationText = "LIVE";
                                }
                                
                                var finalId = (isLive ? "ytlive_" : "") + videoId;
                                
                                var titleEl = el.querySelector('h3, span.compact-media-item-metadata-title');
                                var title = titleEl ? titleEl.innerText.trim() : 'Unknown Title';
                                
                                var authorEl = el.querySelector('div.small-media-item-metadata, span.compact-media-item-channel, div.ytm-badge-and-byline-item');
                                var author = authorEl ? authorEl.innerText.trim() : 'Unknown Artist';
                                
                                var imgEl = el.querySelector('img');
                                var artworkUrl = '';
                                if (imgEl) {
                                    artworkUrl = imgEl.getAttribute('src') || '';
                                    if (artworkUrl.startsWith('data:image') || !artworkUrl) {
                                        artworkUrl = imgEl.getAttribute('data-src') || imgEl.getAttribute('delay-src') || imgEl.getAttribute('thumb') || '';
                                    }
                                }
                                
                                if (videoId && title && title !== 'Unknown Title') {
                                    tracks.push({
                                        id: finalId,
                                        title: title,
                                        artist: author,
                                        artworkUrl: artworkUrl,
                                        durationText: durationText
                                    });
                                }
                            } catch(err) {}
                        });
                    }
                    
                    window.AndroidSearchBridge.onSearchResultsFetched(JSON.stringify(tracks));
                }, 1200);
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    @JavascriptInterface
    fun onSearchResultsFetched(jsonStr: String) {
        val tracks = mutableListOf<Track>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val artist = obj.getString("artist")
                val rawId = id.removePrefix("ytlive_")
                val artworkUrl = "https://i.ytimg.com/vi/$rawId/hqdefault.jpg"
                val durationText = obj.optString("durationText", "0:00")
                
                val durationMs = parseDurationText(durationText)
                val streamUri = Uri.parse("https://inv.zoomerville.com/latest_version?id=$id&itag=140")
                
                tracks.add(
                    Track(
                        id = if (id.startsWith("ytlive_")) id else "yt_$id",
                        title = title,
                        artist = artist,
                        mediaUri = streamUri,
                        artworkUri = artworkUrl,
                        duration = durationMs
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        resultsFlow.value = tracks
        isLoadingFlow.value = false
    }

    private fun parseDurationText(simpleText: String): Long {
        if (simpleText.isEmpty()) return 0L
        val parts = simpleText.split(":")
        var seconds = 0L
        try {
            if (parts.size == 2) {
                seconds = parts[0].toLong() * 60 + parts[1].toLong()
            } else if (parts.size == 3) {
                seconds = parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return seconds * 1000L
    }
}
