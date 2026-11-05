package com.izakji.mccreatorutils.client.browser;

import net.minecraft.client.Minecraft;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefCallback;
import org.cef.handler.CefCookieAccessFilter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

/**
 * Cancels matching advertising and tracking requests in JCEF.
 *
 * @author Izakji
 */
final class BrowserAdBlockHandler implements CefRequestHandler, CefResourceRequestHandler {
    private final BrowserAdBlockList blockList;

    BrowserAdBlockHandler(BrowserAdBlockList blockList) {
        this.blockList = blockList;
    }

    @Override
    public boolean onBeforeBrowse(
            CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture, boolean isRedirect) {
        return false;
    }

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
        return false;
    }

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(
            CefBrowser browser,
            CefFrame frame,
            CefRequest request,
            boolean isNavigation,
            boolean isDownload,
            String requestInitiator,
            BoolRef disableHandler) {
        return this;
    }

    @Override
    public boolean getAuthCredentials(
            CefBrowser browser,
            String originUrl,
            boolean isProxy,
            String host,
            int port,
            String realm,
            String scheme,
            CefAuthCallback callback) {
        return false;
    }

    @Override
    public boolean onCertificateError(
            CefBrowser browser, CefLoadHandler.ErrorCode certError, String requestUrl, CefCallback callback) {
        return false;
    }

    @Override
    public void onRenderProcessTerminated(
            CefBrowser browser, CefRequestHandler.TerminationStatus status, int errorCode, String errorText) {
        // No recovery action; Rinku owns browser lifecycle.
    }

    @Override
    public CefCookieAccessFilter getCookieAccessFilter(CefBrowser browser, CefFrame frame, CefRequest request) {
        return null;
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
        if (blockList.matches(request.getURL())) {
            return true;
        }
        return false;
    }

    @Override
    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
        return null;
    }

    @Override
    public void onResourceRedirect(
            CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, StringRef newUrl) {
        // No redirect rewriting.
    }

    @Override
    public boolean onResourceResponse(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response) {
        return false;
    }

    @Override
    public void onResourceLoadComplete(
            CefBrowser browser,
            CefFrame frame,
            CefRequest request,
            CefResponse response,
            CefURLRequest.Status status,
            long receivedContentLength) {
        if (browser == null
                || frame == null
                || !frame.isValid()
                || !frame.isMain()
                || request == null
                || request.getResourceType() != CefRequest.ResourceType.RT_MAIN_FRAME) {
            return;
        }
        String requestedUrl = request.getURL();
        String sourceUrl = requestedUrl == null ? "" : requestedUrl;
        Minecraft.getInstance().execute(() -> {
            try {
                browser.executeJavaScript(BrowserAdBlockCosmetics.script(), sourceUrl, 0);
            } catch (RuntimeException failure) {
                // The page may have closed between load completion and injection.
            }
        });
    }

    @Override
    public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, BoolRef allow) {
        // No custom protocol handling.
    }
}
