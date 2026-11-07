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

                    // Horizontalni pokret -> ignoriši
                    if (diffX > diffY) {
                        return false;
                    }

                    // Računamo brzinu
                    velocityTracker.computeCurrentVelocity(1000);
                    float yVelocity = velocityTracker.getYVelocity();

                    // ZAŠTITA OD SKROLA SA ODIGNUTIM PRSTOM (FLING-A):
                    // Ako je brzina negativna (kreće se nagore) ili ako sistem detektuje 
                    // da je u pitanju inercijalni skrol, odmah odbacujemo.
                    // Refresh se pali SAMO ako je pomeraj dubok (> 130px) I brzina je u pozitivnom smeru (nadole)
                    if (diffY > 130 && yVelocity > 1500 && yVelocity < 8000) {
                        // Ograničili smo gornju granicu (< 8000) da divlje odbijanje prsta 
                        // pri brzom skrolu ne aktivira lažni refresh!
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
