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
        
        // Boje kružića (#1A1A1C pozadina i #CAD967 strelica)
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1A1A1C"));
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#CAD967"));
        
        if (webView.getParent() != null) {
            ((android.view.ViewGroup) webView.getParent()).removeView(webView);
        }

        swipeRefreshLayout.addView(webView, new CustomSwipeRefreshLayout.LayoutParams(
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
        private boolean pullTriggered = false;
        private final int touchSlop;

        public CustomSwipeRefreshLayout(Context context, WebView webView) {
            super(context);
            this.targetWebView = webView;
            this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean canChildScrollUp() {
            // Ako nismo na vrhu stranice, dozvoljavamo normalan skrol i blokiramo refresh
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                return true;
            }
            // Ako jesmo na vrhu, vraćamo !pullTriggered da kontrolišemo intercepciju
            return !pullTriggered;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = ev.getY();
                    startX = ev.getX();
                    startTime = System.currentTimeMillis();
                    pullTriggered = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Ako je korisnik u međuvremenu skrolovao nadole, prekidamo
                    if (targetWebView != null && targetWebView.getScrollY() > 0) {
                        return false;
                    }

                    float diffY = ev.getY() - startY;
                    float diffX = Math.abs(ev.getX() - startX);
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    // Ako je pokret horizontalan, nije povlačenje nadole
                    if (diffX > diffY) {
                        return false;
                    }

                    // GLAVNI USLOV: Pomeraj > 150px I vreme držanja > 150ms
                    if (diffY > 150 && elapsedTime > 150) {
                        pullTriggered = true;
                    } else if (!pullTriggered) {
                        // Sve dok uslov nije zadovoljen, sprečavamo SwipeRefreshLayout da se aktivira
                        return false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    pullTriggered = false;
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }
}
