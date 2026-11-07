package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
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
            
            // Početno je isključen da sprečimo bilo kakav rad tokom običnog skrola
            setEnabled(false);

            // Direktno pratimo dodire nad WebView-jem nezavisno od sistemskog SwipeRefreshLayout-a
            targetWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Ako nismo na vrhu stranice, osiguravamo da je refresh ugasen
                    if (targetWebView.getScrollY() > 0) {
                        setEnabled(false);
                        return false;
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();
                            startX = event.getX();
                            startTime = System.currentTimeMillis();
                            isIntentionalPull = false;
                            setEnabled(false);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float diffY = event.getY() - startY;
                            float diffX = Math.abs(event.getX() - startX);
                            long elapsedTime = System.currentTimeMillis() - startTime;

                            // Ako je pokret više horizontalan nego vertikalan -> ignoriši
                            if (diffX > diffY) {
                                setEnabled(false);
                                break;
                            }

                            // USLOV: Pomeraj > 150px i vreme > 150ms (drži prst i vuče nadole)
                            if (diffY > 150 && elapsedTime > 150) {
                                isIntentionalPull = true;
                                setEnabled(true); // Tek sada aktiviramo SwipeRefreshLayout da pokaže kružić
                            } else if (!isIntentionalPull) {
                                setEnabled(false);
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isIntentionalPull) {
                                setEnabled(false);
                            }
                            isIntentionalPull = false;
                            break;
                    }
                    
                    // Vraćamo false da WebView i dalje normalno prima i obrađuje svoje standardne dodire i skrol
                    return false;
                }
            });
        }

        @Override
        public boolean canChildScrollUp() {
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                return true;
            }
            return !isIntentionalPull;
        }
    }
}
