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
    private final boolean isEnabled;
    private CustomSwipeRefreshLayout swipeRefreshLayout;

    // Konstruktor prima i podatak da li je pull-to-refresh uključen u podešavanjima
    public PullRefresh(Context context, WebView webView, boolean isEnabled) {
        this.context = context;
        this.webView = webView;
        this.isEnabled = isEnabled;
    }

    public CustomSwipeRefreshLayout setup(android.view.ViewGroup.LayoutParams layoutParams) {
        swipeRefreshLayout = new CustomSwipeRefreshLayout(context, webView, isEnabled);
        
        // Ako je korisnik isključio u podešavanjima, gasimo SwipeRefresh funkcionalnost
        if (!isEnabled) {
            swipeRefreshLayout.setEnabled(false);
        }

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
            if (!isEnabled) return;
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
        private final boolean enabled;
        private float startY = 0f;
        private float startX = 0f;
        private long startTime = 0L;
        private boolean isIntentionalPull = false;
        private boolean isJsAtTop = true; // Podrazumevano dozvoljeno dok JS ne proveri

        public CustomSwipeRefreshLayout(Context context, WebView webView, boolean enabled) {
            super(context);
            this.targetWebView = webView;
            this.enabled = enabled;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            // Ako je pull-to-refresh ugašen u podešavanjima, odmah preskačemo
            if (!enabled) {
                return false;
            }

            // Ako nismo na samom vrhu stranice (glavni prozor), prekidamo
            if (targetWebView != null && targetWebView.getScrollY() > 0) {
                isIntentionalPull = false;
                return false;
            }

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = ev.getY();
                    startX = ev.getX();
                    startTime = System.currentTimeMillis();
                    isIntentionalPull = false;

                    // INJEKCIJA U LETU (Bez diranja MainActivity-ja):
                    // Čim korisnik spusti prst, pitamo sajt preko JS-a (sa podrškom za Shadow DOM)
                    // da li je element na koordinatama dodira skrolovan. Sve je u try-catch bloku da ne može da sruši sajt.
                    if (targetWebView != null) {
                        String jsScript = 
                            "(function() {" +
                            "  if (window.__pullRefreshInjected) return;" +
                            "  window.__pullRefreshInjected = true;" +
                            "  window.canSwipeRefreshAt = function(x, y) {" +
                            "    try {" +
                            "      let element = document.elementFromPoint(x, y);" +
                            "      if (!element) return true;" +
                            "      let current = element;" +
                            "      while (current) {" +
                            "        if (current.nodeType === Node.ELEMENT_NODE) {" +
                            "          let style = window.getComputedStyle(current);" +
                            "          let overflowY = style.getPropertyValue('overflow-y');" +
                            "          let isScrollable = (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay');" +
                            "          if (isScrollable && current.scrollTop > 0) {" +
                            "            return false;" + // Unutrašnji element nije na vrhu!
                            "          }" +
                            "        }" +
                            "        current = current.parentElement || (current.getRootNode && current.getRootNode().host);" +
                            "      }" +
                            "      if (window.scrollY > 0 || document.documentElement.scrollTop > 0) {" +
                            "        return false;" +
                            "      }" +
                            "    } catch (e) {}" +
                            "    return true;" +
                            "  };" +
                            "})();" +
                            "window.canSwipeRefreshAt ? window.canSwipeRefreshAt(" + ev.getX() + ", " + ev.getY() + ") : true;";

                        targetWebView.evaluateJavascript(jsScript, value -> {
                            if (value != null) {
                                isJsAtTop = Boolean.parseBoolean(value);
                            }
                        });
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Ako JS provera kaže da je prst unutar skrolovanog unutrašnjeg elementa -> prekidamo
                    if (!isJsAtTop) {
                        return false;
                    }

                    float diffY = ev.getY() - startY;
                    float diffX = Math.abs(ev.getX() - startX);
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    // Ako je korisnik povukao levo-desno više nego gore-dole -> ignoriši
                    if (diffX > diffY) {
                        return false;
                    }

                    // Tvoj uslov (fallback / zaštita od kratkih pokreta)
                    if (diffY > 150 && elapsedTime > 150) {
                        isIntentionalPull = true;
                    } else if (!isIntentionalPull) {
                        return false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isIntentionalPull = false;
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }
}
