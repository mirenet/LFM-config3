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
        
        // Boje kružića
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

        public CustomSwipeRefreshLayout(Context context, WebView webView) {
            super(context);
            this.targetWebView = webView;
            
            // Fizički gasimo sistem dok korisnik ne napravi pravi gest
            setEnabled(false);

            targetWebView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Ako nismo na vrhu stranice, osiguravamo da je refresh isključen
                    if (targetWebView.getScrollY() > 0) {
                        setEnabled(false);
                        return false;
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();
                            startX = event.getX();
                            startTime = System.currentTimeMillis();
                            setEnabled(false);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float diffY = event.getY() - startY;
                            float diffX = Math.abs(event.getX() - startX);
                            long elapsedTime = System.currentTimeMillis() - startTime;

                            // Ako vuče levo-desno -> ignoriši
                            if (diffX > diffY) {
                                setEnabled(false);
                                break;
                            }

                            // USLOV: Na vrhu smo + pomeraj > 150px + vreme držanja > 150ms
                            if (diffY > 150 && elapsedTime > 150) {
                                setEnabled(true); // Tek sada palimo sistem da prikaže kružić
                            } else if (!isRefreshing()) {
                                setEnabled(false);
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isRefreshing()) {
                                setEnabled(false);
                            }
                            break;
                    }

                    // Vraćamo false da WebView i dalje normalno skroluje i reaguje na klikove
                    return false;
                }
            });
        }

        @Override
        public boolean canChildScrollUp() {
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                return true;
            }
            return !isEnabled();
        }
    }
}
