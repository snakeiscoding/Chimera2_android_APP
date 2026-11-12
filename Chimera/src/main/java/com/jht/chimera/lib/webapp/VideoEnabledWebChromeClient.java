package com.jht.chimera.lib.webapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

// javascript snippet take from 
// http://stackoverflow.com/questions/15768837/playing-html5-video-on-fullscreen-in-android-webview/16179544#16179544

/**
 * This class serves as a WebChromeClient to be set to a WebView, allowing it to
 * play video. Video will play differently depending on target API level
 * (in-line, fullscreen, or both).
 * <p>
 * It has been tested with the following video classes: -
 * android.widget.VideoView (typically API level <11) -
 * android.webkit.HTML5VideoFullScreen$VideoSurfaceView/VideoTextureView
 * (typically API level 11-18) -
 * com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView
 * (typically API level 19+)
 * <p>
 * Important notes: - For API level 11+, android:hardwareAccelerated="true" must
 * be set in the application manifest. - The invoking activity must call
 * VideoEnabledWebChromeClient's onBackPressed() inside of its own
 * onBackPressed(). - Tested in Android API levels 8-19. Only tested on
 * http://m.youtube.com.
 *
 * @author Cristian Perez (http://cpr.name)
 */
public class VideoEnabledWebChromeClient extends WebChromeClient {

    private final View _activityNonVideoView;
    private final ViewGroup _activityVideoView;

    private FrameLayout _videoViewContainer;

    public VideoEnabledWebChromeClient(View activityNonVideoView, ViewGroup activityVideoView) {
        _activityNonVideoView = activityNonVideoView;
        _activityVideoView = activityVideoView;

        activityVideoView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        final String[] permissions = new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID};
        request.grant(permissions);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (view instanceof FrameLayout) {
            // Save video related variables
            _videoViewContainer = (FrameLayout) view;

            // Hide the non-video view, add the video view, and show it
            showFullScreenVideoView();
        }
    }

    private void showFullScreenVideoView() {
        _activityNonVideoView.setVisibility(View.INVISIBLE);
        _activityVideoView.addView(_videoViewContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        _activityVideoView.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
        onShowCustomView(view, callback);
    }

    public void showVideoContainer() {
        if(_videoViewContainer != null) {
            showFullScreenVideoView();
        }
    }

    public void hideVideoContainer() {
        // request focus to fix an issue where virtual keyboard no longer shows on webview but not really sure why this is needed (CHIM-9153)
        _activityVideoView.requestFocus();

        _activityVideoView.setVisibility(View.INVISIBLE);
        _activityVideoView.removeView(_videoViewContainer);
        _activityNonVideoView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onHideCustomView() {
        // This method should be manually called on video end in all cases
        // because it's not always called automatically.
        // This method must be manually called on back key press (from this
        // class' onBackPressed() method).

        // Hide the video view, remove it, and show the non-video view
        hideVideoContainer();


        // Reset video related variables
        _videoViewContainer = null;


    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return Bitmap.createBitmap(
                new int[]{Color.TRANSPARENT}, 1, 1, Bitmap.Config.ARGB_8888);
    }
}