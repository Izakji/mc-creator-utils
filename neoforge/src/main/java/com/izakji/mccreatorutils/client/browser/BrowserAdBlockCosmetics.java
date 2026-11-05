package com.izakji.mccreatorutils.client.browser;

/**
 * Injects page CSS and a DOM sweep that hides leftover advertising containers.
 *
 * <p>Request blocking alone leaves empty frames and reserved slots behind, which can break the
 * layout of pages such as CurseForge. The injected script is idempotent and uses a mutation
 * observer so dynamically inserted slots are hidden as well.
 *
 * @author Izakji
 */
final class BrowserAdBlockCosmetics {
    private static final String SCRIPT = """
            (function () {
              if (window.__mcuAdCosmetics) {
                window.__mcuAdCosmetics();
                return;
              }
              var selectors = [
                '[id^="div-gpt-ad"]',
                '[id^="google_ads"]',
                '[id^="aswift_"]',
                '[id^="gpt-ad"]',
                '[data-google-query-id]',
                '[data-ad]',
                '[data-ad-slot]',
                '[data-ad-unit]',
                '[data-advertisement]',
                '.adsbygoogle',
                '.gpt-ad',
                '.ad',
                '.ads',
                '.ad-slot',
                '.ad-unit',
                '.ad-container',
                '.ad-wrapper',
                '.ad-banner',
                '.advertisement',
                '[class~="ad"]',
                '[class~="ads"]',
                '[class*="ad-slot"]',
                '[class*="ad-unit"]',
                '[class*="ad-container"]',
                '[class*="ad-wrapper"]',
                '[class*="ad-banner"]',
                '[class*="advertisement"]',
                '[aria-label="Advertisement" i]',
                'iframe[src*="doubleclick.net"]',
                'iframe[src*="googlesyndication.com"]',
                'iframe[src*="googleadservices.com"]',
                'iframe[src*="amazon-adsystem.com"]',
                'iframe[src*="criteo."]',
                'iframe[src*="taboola.com"]',
                'iframe[src*="outbrain.com"]',
                'iframe[src*="pubmatic.com"]',
                'iframe[src*="rubiconproject.com"]',
                'iframe[src*="openx.net"]',
                'iframe[src*="casalemedia.com"]',
                'iframe[src*="teads.tv"]',
                'iframe[src*="revcontent.com"]',
                'iframe[src*="mgid.com"]'
              ];
              var selectorText = selectors.join(',');
              var style = document.createElement('style');
              style.id = 'mcu-ad-cosmetics';
              style.textContent = selectorText + '{display:none!important}';
              (document.head || document.documentElement).appendChild(style);
              var blockedHosts = [
                'doubleclick.net',
                'googlesyndication.com',
                'googleadservices.com',
                'google-analytics.com',
                'googletagmanager.com',
                'amazon-adsystem.com',
                'adnxs.com',
                'criteo.',
                'taboola.com',
                'outbrain.com',
                'pubmatic.com',
                'rubiconproject.com',
                'openx.net',
                'casalemedia.com',
                'teads.tv',
                'revcontent.com',
                'mgid.com',
                'adform.net',
                'adroll.com'
              ];
              function hide(element) {
                element.style.setProperty('display', 'none', 'important');
              }
              function isBlocked(source) {
                for (var index = 0; index < blockedHosts.length; index++) {
                  if (source.indexOf(blockedHosts[index]) !== -1) {
                    return true;
                  }
                }
                return false;
              }
              function hideBlockedFrame(frame) {
                var source = (frame.getAttribute('src') || '').toLowerCase();
                if (!source || !isBlocked(source)) {
                  return;
                }
                hide(frame);
                var parent = frame.parentElement;
                if (parent && parent.children.length === 1 && !parent.textContent.trim()) {
                  hide(parent);
                }
              }
              function sweepRoot(root) {
                if (!root || root.nodeType !== 1) {
                  return;
                }
                if (root.matches(selectorText)) {
                  hide(root);
                }
                var matches = root.querySelectorAll(selectorText);
                for (var matchIndex = 0; matchIndex < matches.length; matchIndex++) {
                  hide(matches[matchIndex]);
                }
                if (root.tagName === 'IFRAME') {
                  hideBlockedFrame(root);
                }
                var frames = root.querySelectorAll('iframe');
                for (var index = 0; index < frames.length; index++) {
                  hideBlockedFrame(frames[index]);
                }
              }
              new MutationObserver(function (records) {
                for (var recordIndex = 0; recordIndex < records.length; recordIndex++) {
                  var added = records[recordIndex].addedNodes;
                  for (var nodeIndex = 0; nodeIndex < added.length; nodeIndex++) {
                    sweepRoot(added[nodeIndex]);
                  }
                }
              }).observe(document.documentElement, {
                childList: true,
                subtree: true
              });
              window.__mcuAdCosmetics = function () {
                sweepRoot(document.documentElement);
              };
              window.__mcuAdCosmetics();
            })();
            """;

    private BrowserAdBlockCosmetics() {}

    static String script() {
        return SCRIPT;
    }
}
