package com.cstestforge.recorder.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service to track DOM mutations and ensure recorder script stays injected
 * even in Single Page Applications (SPAs) where the DOM may be rebuilt.
 */
@Service
public class MutationObserverService {
    private static final Logger logger = LoggerFactory.getLogger(MutationObserverService.class);
    
    @Autowired
    private BrowserManager browserManager;
    
    // JavaScript code for the mutation observer
    private static final String MUTATION_OBSERVER_SCRIPT = 
            "(function() {\n" +
            "  if (window.__csMutationObserver) {\n" +
            "    console.debug('CSTestForge: Mutation observer already exists');\n" +
            "    return;\n" +
            "  }\n" +
            "\n" +
            "  console.debug('CSTestForge: Setting up mutation observer');\n" +
            "  \n" +
            "  // Track if the recorder script is present\n" +
            "  window.__csRecorderScriptPresent = true;\n" +
            "  \n" +
            "  // Track SPA navigation\n" +
            "  window.__csLastUrl = window.location.href;\n" +
            "  \n" +
            "  // Function to check if recorder script is still active\n" +
            "  function checkRecorderScript() {\n" +
            "    if (!window.__csRecorderActive) {\n" +
            "      console.debug('CSTestForge: Recorder script not active, requesting reinjection');\n" +
            "      window.__csRecorderScriptPresent = false;\n" +
            "      window.dispatchEvent(new CustomEvent('cs_recorder_script_needed'));\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  // Function to handle URL changes in SPAs\n" +
            "  function checkUrlChange() {\n" +
            "    if (window.location.href !== window.__csLastUrl) {\n" +
            "      console.debug('CSTestForge: URL changed: ' + window.location.href);\n" +
            "      window.__csLastUrl = window.location.href;\n" +
            "      window.dispatchEvent(new CustomEvent('cs_url_changed', {\n" +
            "        detail: { url: window.location.href }\n" +
            "      }));\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  // Check for SPA frameworks\n" +
            "  function detectSpaFramework() {\n" +
            "    const frameworks = {\n" +
            "      'react': !!(window.React || window._REACT_DEVTOOLS_GLOBAL_HOOK__),\n" +
            "      'angular': !!(window.angular || window.ng),\n" +
            "      'vue': !!(window.Vue || document.querySelector('[data-v-app]')),\n" +
            "      'ember': !!(window.Ember),\n" +
            "      'backbone': !!(window.Backbone),\n" +
            "      'polymer': !!(window.Polymer)\n" +
            "    };\n" +
            "    \n" +
            "    for (const [name, detected] of Object.entries(frameworks)) {\n" +
            "      if (detected) {\n" +
            "        console.debug('CSTestForge: Detected ' + name + ' framework');\n" +
            "        window.__csSpaFramework = name;\n" +
            "        return name;\n" +
            "      }\n" +
            "    }\n" +
            "    \n" +
            "    return null;\n" +
            "  }\n" +
            "  \n" +
            "  // Set up specific framework listeners\n" +
            "  function setupFrameworkSpecificListeners() {\n" +
            "    const framework = detectSpaFramework();\n" +
            "    \n" +
            "    if (framework === 'react') {\n" +
            "      // For React, we need to observe DOM changes\n" +
            "      // React often rebuilds parts of the DOM\n" +
            "    } else if (framework === 'angular') {\n" +
            "      // For Angular, we can listen to router events\n" +
            "      if (window.angular) {\n" +
            "        try {\n" +
            "          const $injector = window.angular.element(document).injector();\n" +
            "          if ($injector) {\n" +
            "            const $rootScope = $injector.get('$rootScope');\n" +
            "            $rootScope.$on('$routeChangeSuccess', function() {\n" +
            "              checkUrlChange();\n" +
            "              checkRecorderScript();\n" +
            "            });\n" +
            "          }\n" +
            "        } catch (e) {\n" +
            "          console.debug('CSTestForge: Error setting up Angular listeners: ' + e.message);\n" +
            "        }\n" +
            "      }\n" +
            "    } else if (framework === 'vue') {\n" +
            "      // For Vue, we need to observe DOM changes\n" +
            "      // Vue often rebuilds parts of the DOM\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  // Set up history API listeners for all SPAs\n" +
            "  function setupHistoryListeners() {\n" +
            "    const originalPushState = window.history.pushState;\n" +
            "    const originalReplaceState = window.history.replaceState;\n" +
            "    \n" +
            "    window.history.pushState = function() {\n" +
            "      originalPushState.apply(this, arguments);\n" +
            "      setTimeout(function() {\n" +
            "        checkUrlChange();\n" +
            "        checkRecorderScript();\n" +
            "      }, 100);\n" +
            "    };\n" +
            "    \n" +
            "    window.history.replaceState = function() {\n" +
            "      originalReplaceState.apply(this, arguments);\n" +
            "      setTimeout(function() {\n" +
            "        checkUrlChange();\n" +
            "        checkRecorderScript();\n" +
            "      }, 100);\n" +
            "    };\n" +
            "    \n" +
            "    window.addEventListener('popstate', function() {\n" +
            "      setTimeout(function() {\n" +
            "        checkUrlChange();\n" +
            "        checkRecorderScript();\n" +
            "      }, 100);\n" +
            "    });\n" +
            "    \n" +
            "    // Also check on hash changes for older SPA implementations\n" +
            "    window.addEventListener('hashchange', function() {\n" +
            "      setTimeout(function() {\n" +
            "        checkUrlChange();\n" +
            "        checkRecorderScript();\n" +
            "      }, 100);\n" +
            "    });\n" +
            "  }\n" +
            "  \n" +
            "  // Create the mutation observer\n" +
            "  window.__csMutationObserver = new MutationObserver(function(mutations) {\n" +
            "    let needsCheck = false;\n" +
            "    \n" +
            "    for (const mutation of mutations) {\n" +
            "      // Check for script removal\n" +
            "      if (mutation.type === 'childList' && mutation.removedNodes.length > 0) {\n" +
            "        for (let i = 0; i < mutation.removedNodes.length; i++) {\n" +
            "          const node = mutation.removedNodes[i];\n" +
            "          if (node.nodeName === 'SCRIPT' && node.id === 'cs-recorder-script') {\n" +
            "            console.debug('CSTestForge: Recorder script was removed');\n" +
            "            window.__csRecorderScriptPresent = false;\n" +
            "            needsCheck = true;\n" +
            "            break;\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "      \n" +
            "      // Check for significant DOM changes that might indicate a SPA navigation\n" +
            "      if (mutation.type === 'childList' && \n" +
            "          (mutation.target === document.body || mutation.target === document.documentElement)) {\n" +
            "        if (mutation.addedNodes.length > 3 || mutation.removedNodes.length > 3) {\n" +
            "          needsCheck = true;\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "    \n" +
            "    if (needsCheck) {\n" +
            "      checkUrlChange();\n" +
            "      checkRecorderScript();\n" +
            "    }\n" +
            "  });\n" +
            "  \n" +
            "  // Start observing\n" +
            "  window.__csMutationObserver.observe(document, {\n" +
            "    childList: true,\n" +
            "    subtree: true,\n" +
            "    attributes: false,\n" +
            "    characterData: false\n" +
            "  });\n" +
            "  \n" +
            "  // Set up framework-specific listeners\n" +
            "  setupFrameworkSpecificListeners();\n" +
            "  \n" +
            "  // Set up history API listeners\n" +
            "  setupHistoryListeners();\n" +
            "  \n" +
            "  // Set up iframe handling\n" +
            "  function setupIframeHandling() {\n" +
            "    // Function to process iframes\n" +
            "    function processIframes() {\n" +
            "      const iframes = document.querySelectorAll('iframe');\n" +
            "      iframes.forEach(function(iframe) {\n" +
            "        try {\n" +
            "          // Skip if iframe is from a different origin\n" +
            "          if (iframe.contentDocument) {\n" +
            "            // Check if recorder script is present in iframe\n" +
            "            if (!iframe.contentWindow.__csRecorderActive) {\n" +
            "              console.debug('CSTestForge: Iframe detected without recorder script');\n" +
            "              window.dispatchEvent(new CustomEvent('cs_iframe_detected', {\n" +
            "                detail: { iframe: iframe }\n" +
            "              }));\n" +
            "            }\n" +
            "          }\n" +
            "        } catch (e) {\n" +
            "          // Cross-origin iframe, can't access\n" +
            "          console.debug('CSTestForge: Cross-origin iframe detected');\n" +
            "        }\n" +
            "      });\n" +
            "    }\n" +
            "    \n" +
            "    // Process iframes initially\n" +
            "    processIframes();\n" +
            "    \n" +
            "    // Set up observer for new iframes\n" +
            "    const iframeObserver = new MutationObserver(function(mutations) {\n" +
            "      let hasNewIframe = false;\n" +
            "      \n" +
            "      mutations.forEach(function(mutation) {\n" +
            "        if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
            "          for (let i = 0; i < mutation.addedNodes.length; i++) {\n" +
            "            const node = mutation.addedNodes[i];\n" +
            "            if (node.nodeName === 'IFRAME' || \n" +
            "                (node.nodeType === Node.ELEMENT_NODE && node.querySelector('iframe'))) {\n" +
            "              hasNewIframe = true;\n" +
            "              break;\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      });\n" +
            "      \n" +
            "      if (hasNewIframe) {\n" +
            "        setTimeout(processIframes, 500); // Wait for iframe to load\n" +
            "      }\n" +
            "    });\n" +
            "    \n" +
            "    iframeObserver.observe(document, {\n" +
            "      childList: true,\n" +
            "      subtree: true,\n" +
            "      attributes: false\n" +
            "    });\n" +
            "  }\n" +
            "  \n" +
            "  // Set up iframe handling\n" +
            "  setupIframeHandling();\n" +
            "  \n" +
            "  console.debug('CSTestForge: Mutation observer setup complete');\n" +
            "})();";
    
    /**
     * Install the mutation observer in the browser
     *
     * @param sessionId The recording session ID
     * @return true if the observer was installed successfully
     */
    public boolean installMutationObserver(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }
        
        try {
            Object result = browserManager.executeScript(sessionId, MUTATION_OBSERVER_SCRIPT);
            logger.debug("Installed mutation observer for session: {}", sessionId);
            return result != null;
        } catch (Exception e) {
            logger.error("Failed to install mutation observer for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the recorder script is still present in the browser
     *
     * @param sessionId The recording session ID
     * @return true if the script is present
     */
    public boolean isRecorderScriptPresent(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }
        
        try {
            Object result = browserManager.executeScript(sessionId, 
                    "return window.__csRecorderActive === true;");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to check recorder script for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle a SPA navigation event
     *
     * @param sessionId The recording session ID
     * @param url The new URL
     */
    public void handleSpaNavigation(UUID sessionId, String url) {
        if (sessionId == null) {
            return;
        }
        
        logger.debug("SPA navigation detected for session {}: {}", sessionId, url);
        
        // Re-inject the recorder script
        browserManager.injectRecorderScript(sessionId);
        
        // Re-install the mutation observer
        installMutationObserver(sessionId);
    }
    
    /**
     * Handle an iframe detection event
     *
     * @param sessionId The recording session ID
     * @param iframeSelector CSS selector for the iframe
     * @return true if the script was injected into the iframe
     */
    public boolean handleIframeDetection(UUID sessionId, String iframeSelector) {
        if (sessionId == null || iframeSelector == null || iframeSelector.isEmpty()) {
            return false;
        }
        
        try {
            // Get the recorder script
            String script = "const iframe = document.querySelector('" + iframeSelector + "');\n" +
                    "if (!iframe || !iframe.contentWindow) return false;\n" +
                    "try {\n" +
                    "  // Check if we can access the iframe (same-origin policy)\n" +
                    "  iframe.contentDocument;\n" +
                    "  return true;\n" +
                    "} catch (e) {\n" +
                    "  console.error('Cannot access iframe: ' + e.message);\n" +
                    "  return false;\n" +
                    "}";
            
            Object canAccess = browserManager.executeScript(sessionId, script);
            if (!Boolean.TRUE.equals(canAccess)) {
                logger.warn("Cannot access iframe for session {}: cross-origin restriction", sessionId);
                return false;
            }
            
            // Inject the recorder script into the iframe
            String injectScript = "const iframe = document.querySelector('" + iframeSelector + "');\n" +
                    "if (!iframe || !iframe.contentWindow) return false;\n" +
                    "const doc = iframe.contentDocument;\n" +
                    "const script = doc.createElement('script');\n" +
                    "script.id = 'cs-recorder-script';\n" +
                    "script.textContent = window.__csRecorderScript;\n" +
                    "doc.head.appendChild(script);\n" +
                    "return true;";
            
            Object result = browserManager.executeScript(sessionId, injectScript);
            logger.debug("Injected recorder script into iframe for session {}: {}", sessionId, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to inject recorder script into iframe for session {}: {}", 
                    sessionId, e.getMessage());
            return false;
        }
    }
} 