package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
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
        private boolean isIntentionalPull = false;

        public CustomSwipeRefreshLayout(Context context, WebView webView) {
            super(context);
            this.targetWebView = webView;
            
            // Po default-u je isključen da spreči bilo kakvo paljenje na obične pokrete i skrol
            setEnabled(false);

            // Prati dodire direktno na WebView-ju da izbegnemo sistemske konflikte sa dva layouth-a
            targetWebView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();
                            startX = event.getX();
                            startTime = System.currentTimeMillis();
                            isIntentionalPull = false;
                            setEnabled(false); // Na svaki novi dodir gasimo refresh
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float diffY = event.getY() - startY;
                            float diffX = Math.abs(event.getX() - startX);
                            long elapsedTime = System.currentTimeMillis() - startTime;

                            // Ako je pokret horizontalan (levo-desno) -> ignoriši
                            if (diffX > diffY) {
                                setEnabled(false);
                                break;
                            }

                            // USLOV: Pomeraj veći od 150px i vreme duže od 150ms (sitan dug dodir i povlačenje nadole)
                            if (diffY > 150 && elapsedTime > 150) {
                                isIntentionalPull = true;
                                setEnabled(true); // Tek sada palimo SwipeRefreshLayout
                            } else if (!isIntentionalPull) {
                                setEnabled(false);
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isIntentionalPull && !isRefreshing()) {
                                setEnabled(false);
                            }
                            isIntentionalPull = false;
                            break;
                    }

                    // Vraćamo false da WebView normalno obavlja skrolovanje i klikove
                    return false;
                }
            });
        }

        @Override
        public boolean canChildScrollUp() {
            // Bez ikakvih provera WebView scroll-a, oslanjamo se čisto na naš isIntentionalPull fleg
            return !isIntentionalPull;
        }
    }
}
