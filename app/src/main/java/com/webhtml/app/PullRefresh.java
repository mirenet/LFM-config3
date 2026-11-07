package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class PullRefresh {

    private final Context context;
    private final WebView webView;
    private CustomSwipeRefreshLayout swipeRefreshLayout;

    public PullRefresh(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    public CustomSwipeRefreshLayout setup(android.view.ViewGroup.LayoutParams layoutParams) {
        swipeRefreshLayout = new CustomSwipeRefreshLayout(context, webView);
        
        // Boje kružića: pozadina i strelica
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1A1A1C"));
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#CAD967"));

        if (webView.getParent() != null) {
            ((android.view.ViewGroup) webView.getParent()).removeView(webView);
        }

        swipeRefreshLayout.addView(webView, new SwipeRefreshLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (layoutParams != null) {
            swipeRefreshLayout.setLayoutParams(layoutParams);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 1500);
        });

        return swipeRefreshLayout;
    }

    public static class CustomSwipeRefreshLayout extends SwipeRefreshLayout {
        private final WebView targetWebView;
        private float startY = 0f;
        private float startX = 0f;
        private long startTime = 0L;
        private boolean isIntentionalPull = false;

        public CustomSwipeRefreshLayout(Context context, WebView webView) {
            super(context);
            this.targetWebView = webView;
            // Početno onemogućeno da sprečimo bilo kakvo neželjeno aktiviranje pri skrolovanju
            setEnabled(false);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                setEnabled(false);
                return false;
            }

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = ev.getY();
                    startX = ev.getX();
                    startTime = System.currentTimeMillis();
                    isIntentionalPull = false;
                    setEnabled(false);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float diffY = ev.getY() - startY;
                    float diffX = Math.abs(ev.getX() - startX);
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    if (diffX > diffY) {
                        setEnabled(false);
                        return false;
                    }

                    // Tek kada korisnik svesno drži i povuče više od 150px i duže od 150ms, 
                    // dozvoljavamo SwipeRefreshLayout-u da se aktivira
                    if (diffY > 150 && elapsedTime > 150) {
                        isIntentionalPull = true;
                        setEnabled(true);
                    } else {
                        setEnabled(false);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isIntentionalPull = false;
                    setEnabled(false);
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }
}
