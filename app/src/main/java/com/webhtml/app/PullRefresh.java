package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
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
        
        // --- PODEŠAVANJE BOJA KRUŽIĆA ---
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

    /**
     * Custom SwipeRefreshLayout sa proverom vrha stranice (scrollY == 0) 
     * i VelocityTracker-om koji reaguje samo na brz i oštar trzaj nadole.
     */
    public static class CustomSwipeRefreshLayout extends SwipeRefreshLayout {
        private final WebView targetWebView;
        private float startY = 0f;
        private float startX = 0f;
        private boolean isIntentionalPull = false;
        private VelocityTracker velocityTracker = null;
        private final int touchSlop;

        public CustomSwipeRefreshLayout(Context context, WebView webView) {
            super(context);
            this.targetWebView = webView;
            this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            // Inicijalizujemo i pratimo brzinu pokreta prsta
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(ev);

            // Ako nismo na samom vrhu stranice, odmah prekidamo
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                isIntentionalPull = false;
                return false;
            }

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = ev.getY();
                    startX = ev.getX();
                    isIntentionalPull = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (targetWebView != null && targetWebView.getScrollY() > 0) {
                        return false;
                    }

                    float diffY = ev.getY() - startY;
                    float diffX = Math.abs(ev.getX() - startX);

                    // Ako je pokret horizontalan -> ignoriši
                    if (diffX > diffY) {
                        return false;
                    }

                    // Računamo brzinu pokreta u pikselima po sekundi
                    velocityTracker.computeCurrentVelocity(1000);
                    float yVelocity = velocityTracker.getYVelocity();

                    // USLOV: 
                    // 1. Pomeraj nadole mora biti veći od 120 piksela
                    // 2. Brzina povlačenja mora biti veća od 700 px/s (što znači brz, oštar trzaj)
                    if (diffY > 120 && yVelocity > 700) {
                        isIntentionalPull = true;
                    } else if (!isIntentionalPull) {
                        return false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isIntentionalPull = false;
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }
}
