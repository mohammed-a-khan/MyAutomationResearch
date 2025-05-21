package com.cstestforge.recorder.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service to track DOM mutations and ensure recorder script stays injected
 * even in Single Page Applications (SPAs) where the DOM may be rebuilt.
 * Enhanced with advanced detection and monitoring capabilities.
 */
@Service
public class MutationObserverService {
    private static final Logger logger = LoggerFactory.getLogger(MutationObserverService.class);

    @Autowired
    private BrowserManager browserManager;

    // Track observer status per session
    private final Map<UUID, ObserverStatus> observerStatuses = new ConcurrentHashMap<>();

    // Schedule periodic health checks
    private ScheduledExecutorService healthCheckExecutor;

    // Static class to track observer status
    private static class ObserverStatus {
        boolean installed = false;
        long lastChecked = 0;
        int retryCount = 0;
        long installTime = 0;
        String currentUrl = "";

        ObserverStatus() {
            this.lastChecked = System.currentTimeMillis();
        }
    }

    @PostConstruct
    public void init() {
        healthCheckExecutor = Executors.newScheduledThreadPool(1);
        healthCheckExecutor.scheduleAtFixedRate(this::performHealthChecks, 30, 15, TimeUnit.SECONDS);
        logger.info("MutationObserverService initialized with scheduled health checks");
    }

    @PreDestroy
    public void shutdown() {
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("MutationObserverService shut down");
    }

    /**
     * Enhanced mutation observer script with SPA framework detection and advanced monitoring
     * Updated with safer node removal to fix null property access errors
     */
    private static final String MUTATION_OBSERVER_SCRIPT =
            "(function() {\n" +
                    "  // Don't install multiple observers\n" +
                    "  if (window.__csMutationObserver) {\n" +
                    "    console.debug('CSTestForge: Mutation observer already exists');\n" +
                    "    return { status: 'already_installed' };\n" +
                    "  }\n" +
                    "\n" +
                    "  console.debug('CSTestForge: Setting up advanced mutation observer');\n" +
                    "  \n" +
                    "  // Track if the recorder script is present\n" +
                    "  window.__csRecorderScriptPresent = true;\n" +
                    "  window.__csLastMutationTime = Date.now();\n" +
                    "  window.__csRecorderHealthStatus = 'ok';\n" +
                    "  window.__csRecorderErrors = [];\n" +
                    "  \n" +
                    "  // Track SPA navigation\n" +
                    "  window.__csLastUrl = window.location.href;\n" +
                    "  window.__csLastUrlChangeTime = Date.now();\n" +
                    "  window.__csPageTitle = document.title;\n" +
                    "  \n" +
                    "  // Track initialization for later audits\n" +
                    "  window.__csMutationObserverStartTime = Date.now();\n" +
                    "  window.__csMutationObserverEventCounts = {\n" +
                    "    mutations: 0,\n" +
                    "    domChanges: 0,\n" +
                    "    urlChanges: 0,\n" +
                    "    scriptReinjects: 0,\n" +
                    "    errors: 0\n" +
                    "  };\n" +
                    "  \n" +
                    "  // Advanced detection of SPA frameworks\n" +
                    "  function detectSpaFramework() {\n" +
                    "    const frameworks = {\n" +
                    "      'react': !!(window.React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__ || document.querySelector('[data-reactroot]')),\n" +
                    "      'angular': !!(window.angular || window.ng || document.querySelector('[ng-app]') || document.querySelector('[ng-controller]')),\n" +
                    "      'vue': !!(window.Vue || document.querySelector('[data-v-app]') || document.querySelector('[data-v-]')),\n" +
                    "      'ember': !!(window.Ember),\n" +
                    "      'backbone': !!(window.Backbone),\n" +
                    "      'polymer': !!(window.Polymer),\n" +
                    "      'next': !!(document.querySelector('#__next')),\n" +
                    "      'nuxt': !!(document.querySelector('#__nuxt')),\n" +
                    "      'svelte': !!(document.querySelector('svelte-component')),\n" +
                    "      'htmx': !!(document.querySelector('[hx-get], [hx-post], [hx-put], [hx-delete]')),\n" +
                    "      'jquery': !!(window.jQuery || window.$)\n" +
                    "    };\n" +
                    "    \n" +
                    "    let detectedFrameworks = [];\n" +
                    "    for (const [name, detected] of Object.entries(frameworks)) {\n" +
                    "      if (detected) {\n" +
                    "        detectedFrameworks.push(name);\n" +
                    "        console.debug('CSTestForge: Detected ' + name + ' framework');\n" +
                    "      }\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (detectedFrameworks.length > 0) {\n" +
                    "      window.__csSpaFrameworks = detectedFrameworks;\n" +
                    "      return detectedFrameworks;\n" +
                    "    }\n" +
                    "    \n" +
                    "    return null;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Function to check if recorder script is still active\n" +
                    "  function checkRecorderScript() {\n" +
                    "    if (typeof window.__csRecorderActive === 'undefined' || window.__csRecorderActive !== true) {\n" +
                    "      console.debug('CSTestForge: Recorder script not active, requesting reinjection');\n" +
                    "      window.__csRecorderScriptPresent = false;\n" +
                    "      window.__csRecorderHealthStatus = 'missing';\n" +
                    "      window.__csMutationObserverEventCounts.scriptReinjects++;\n" +
                    "      \n" +
                    "      try {\n" +
                    "        window.dispatchEvent(new CustomEvent('cs_recorder_script_needed', {\n" +
                    "          detail: {\n" +
                    "            url: window.location.href,\n" +
                    "            timestamp: Date.now(),\n" +
                    "            reason: 'script_not_active'\n" +
                    "          }\n" +
                    "        }));\n" +
                    "      } catch(e) {\n" +
                    "        console.error('CSTestForge: Error dispatching script needed event:', e);\n" +
                    "        window.__csRecorderErrors.push({ time: Date.now(), error: e.toString(), context: 'checkRecorderScript' });\n" +
                    "        window.__csMutationObserverEventCounts.errors++;\n" +
                    "      }\n" +
                    "      \n" +
                    "      return false;\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Also check if UI indicator is present\n" +
                    "    const uiIndicator = document.getElementById('cs-recorder-indicator');\n" +
                    "    if (!uiIndicator) {\n" +
                    "      console.debug('CSTestForge: Recorder UI not present, requesting recreation');\n" +
                    "      window.__csRecorderHealthStatus = 'ui_missing';\n" +
                    "      \n" +
                    "      try {\n" +
                    "        window.dispatchEvent(new CustomEvent('cs_recorder_ui_needed', {\n" +
                    "          detail: {\n" +
                    "            url: window.location.href,\n" +
                    "            timestamp: Date.now()\n" +
                    "          }\n" +
                    "        }));\n" +
                    "      } catch(e) {\n" +
                    "        console.error('CSTestForge: Error dispatching UI needed event:', e);\n" +
                    "        window.__csRecorderErrors.push({ time: Date.now(), error: e.toString(), context: 'checkRecorderUI' });\n" +
                    "        window.__csMutationObserverEventCounts.errors++;\n" +
                    "      }\n" +
                    "      \n" +
                    "      return false;\n" +
                    "    }\n" +
                    "    \n" +
                    "    return true;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Enhanced function to handle URL changes in SPAs with better detection\n" +
                    "  function checkUrlChange() {\n" +
                    "    const currentUrl = window.location.href;\n" +
                    "    const currentTitle = document.title;\n" +
                    "    \n" +
                    "    if (currentUrl !== window.__csLastUrl) {\n" +
                    "      console.debug('CSTestForge: URL changed from ' + window.__csLastUrl + ' to ' + currentUrl);\n" +
                    "      window.__csMutationObserverEventCounts.urlChanges++;\n" +
                    "      window.__csLastUrlChangeTime = Date.now();\n" +
                    "      \n" +
                    "      // Store previous and current values\n" +
                    "      const prevUrl = window.__csLastUrl;\n" +
                    "      window.__csLastUrl = currentUrl;\n" +
                    "      window.__csPageTitle = currentTitle;\n" +
                    "      \n" +
                    "      // Send navigation event\n" +
                    "      try {\n" +
                    "        window.dispatchEvent(new CustomEvent('cs_url_changed', {\n" +
                    "          detail: {\n" +
                    "            prevUrl: prevUrl,\n" +
                    "            newUrl: currentUrl,\n" +
                    "            title: currentTitle,\n" +
                    "            timestamp: Date.now()\n" +
                    "          }\n" +
                    "        }));\n" +
                    "      } catch(e) {\n" +
                    "        console.error('CSTestForge: Error dispatching URL change event:', e);\n" +
                    "        window.__csRecorderErrors.push({ time: Date.now(), error: e.toString(), context: 'checkUrlChange' });\n" +
                    "        window.__csMutationObserverEventCounts.errors++;\n" +
                    "      }\n" +
                    "      \n" +
                    "      // URL changes often require script reinjection\n" +
                    "      setTimeout(checkRecorderScript, 500);\n" +
                    "      return true;\n" +
                    "    } else if (currentTitle !== window.__csPageTitle) {\n" +
                    "      // Title change might indicate a soft navigation in some SPAs\n" +
                    "      console.debug('CSTestForge: Page title changed from \"' + window.__csPageTitle + '\" to \"' + currentTitle + '\"');\n" +
                    "      window.__csPageTitle = currentTitle;\n" +
                    "      \n" +
                    "      // Do a recorder check as well on title changes\n" +
                    "      setTimeout(checkRecorderScript, 500);\n" +
                    "      return true;\n" +
                    "    }\n" +
                    "    \n" +
                    "    return false;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Set up framework-specific listeners\n" +
                    "  function setupFrameworkSpecificListeners() {\n" +
                    "    const detectedFrameworks = detectSpaFramework();\n" +
                    "    \n" +
                    "    if (!detectedFrameworks || detectedFrameworks.length === 0) {\n" +
                    "      return;\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Angular-specific handling\n" +
                    "    if (detectedFrameworks.includes('angular')) {\n" +
                    "      if (window.angular) {\n" +
                    "        try {\n" +
                    "          const $injector = window.angular.element(document).injector();\n" +
                    "          if ($injector) {\n" +
                    "            const $rootScope = $injector.get('$rootScope');\n" +
                    "            $rootScope.$on('$routeChangeSuccess', function() {\n" +
                    "              console.debug('CSTestForge: Angular route change detected');\n" +
                    "              checkUrlChange();\n" +
                    "              checkRecorderScript();\n" +
                    "            });\n" +
                    "            \n" +
                    "            $rootScope.$on('$stateChangeSuccess', function() {\n" +
                    "              console.debug('CSTestForge: Angular UI-Router state change detected');\n" +
                    "              checkUrlChange();\n" +
                    "              checkRecorderScript();\n" +
                    "            });\n" +
                    "          }\n" +
                    "        } catch (e) {\n" +
                    "          console.debug('CSTestForge: Error setting up Angular listeners: ' + e.message);\n" +
                    "          window.__csRecorderErrors.push({ time: Date.now(), error: e.toString(), context: 'angularSetup' });\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "    \n" +
                    "    // React specific handling\n" +
                    "    if (detectedFrameworks.includes('react')) {\n" +
                    "      // For React, we set up more aggressive mutation observation\n" +
                    "      console.debug('CSTestForge: Setting up enhanced React monitoring');\n" +
                    "      \n" +
                    "      // Look for critical React containers\n" +
                    "      const reactRoots = document.querySelectorAll('[data-reactroot], #root, #app, .react-root');\n" +
                    "      if (reactRoots.length > 0) {\n" +
                    "        reactRoots.forEach(root => {\n" +
                    "          // Set up specific observer for React root with more sensitivity\n" +
                    "          const reactObserver = new MutationObserver(mutations => {\n" +
                    "            window.__csLastMutationTime = Date.now();\n" +
                    "            window.__csMutationObserverEventCounts.mutations++;\n" +
                    "            \n" +
                    "            // React often rebuilds large portions of the DOM\n" +
                    "            let significantChanges = false;\n" +
                    "            for (const mutation of mutations) {\n" +
                    "              if (mutation.type === 'childList' && \n" +
                    "                  (mutation.addedNodes.length > 0 || mutation.removedNodes.length > 0)) {\n" +
                    "                significantChanges = true;\n" +
                    "                window.__csMutationObserverEventCounts.domChanges++;\n" +
                    "                break;\n" +
                    "              }\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (significantChanges) {\n" +
                    "              console.debug('CSTestForge: Significant React DOM changes detected');\n" +
                    "              // Check URL changes first\n" +
                    "              checkUrlChange();\n" +
                    "              // Then verify recorder script after a short delay\n" +
                    "              setTimeout(checkRecorderScript, 200);\n" +
                    "            }\n" +
                    "          });\n" +
                    "          \n" +
                    "          // Observe with more detailed options\n" +
                    "          reactObserver.observe(root, {\n" +
                    "            childList: true,\n" +
                    "            subtree: true,\n" +
                    "            attributes: false\n" +
                    "          });\n" +
                    "        });\n" +
                    "      }\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Vue specific handling\n" +
                    "    if (detectedFrameworks.includes('vue')) {\n" +
                    "      console.debug('CSTestForge: Setting up Vue specific monitoring');\n" +
                    "      \n" +
                    "      // Look for Vue app root elements\n" +
                    "      const vueRoots = document.querySelectorAll('[data-v-app], #app, .vue-app');\n" +
                    "      if (vueRoots.length > 0) {\n" +
                    "        vueRoots.forEach(root => {\n" +
                    "          // Similar to React handling\n" +
                    "          const vueObserver = new MutationObserver(mutations => {\n" +
                    "            window.__csLastMutationTime = Date.now();\n" +
                    "            window.__csMutationObserverEventCounts.mutations++;\n" +
                    "            \n" +
                    "            let significantChanges = false;\n" +
                    "            for (const mutation of mutations) {\n" +
                    "              if (mutation.type === 'childList' && \n" +
                    "                  (mutation.addedNodes.length > 0 || mutation.removedNodes.length > 0)) {\n" +
                    "                significantChanges = true;\n" +
                    "                window.__csMutationObserverEventCounts.domChanges++;\n" +
                    "                break;\n" +
                    "              }\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (significantChanges) {\n" +
                    "              console.debug('CSTestForge: Significant Vue DOM changes detected');\n" +
                    "              checkUrlChange();\n" +
                    "              setTimeout(checkRecorderScript, 200);\n" +
                    "            }\n" +
                    "          });\n" +
                    "          \n" +
                    "          vueObserver.observe(root, {\n" +
                    "            childList: true,\n" +
                    "            subtree: true,\n" +
                    "            attributes: false\n" +
                    "          });\n" +
                    "        });\n" +
                    "      }\n" +
                    "    }\n" +
                    "    \n" +
                    "    // HTMX specific handling\n" +
                    "    if (detectedFrameworks.includes('htmx')) {\n" +
                    "      // Listen to HTMX events\n" +
                    "      document.addEventListener('htmx:afterSwap', function(e) {\n" +
                    "        console.debug('CSTestForge: HTMX content swap detected');\n" +
                    "        setTimeout(checkRecorderScript, 200);\n" +
                    "      });\n" +
                    "      \n" +
                    "      document.addEventListener('htmx:afterRequest', function(e) {\n" +
                    "        console.debug('CSTestForge: HTMX request completed');\n" +
                    "        setTimeout(checkUrlChange, 100);\n" +
                    "      });\n" +
                    "    }\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Enhanced history API listeners for all SPAs\n" +
                    "  function setupHistoryListeners() {\n" +
                    "    if (window.__csHistoryListenersInstalled) {\n" +
                    "      return;\n" +
                    "    }\n" +
                    "    \n" +
                    "    window.__csHistoryListenersInstalled = true;\n" +
                    "    \n" +
                    "    const originalPushState = window.history.pushState;\n" +
                    "    const originalReplaceState = window.history.replaceState;\n" +
                    "    \n" +
                    "    window.history.pushState = function() {\n" +
                    "      console.debug('CSTestForge: history.pushState detected');\n" +
                    "      \n" +
                    "      // Call original method first\n" +
                    "      const result = originalPushState.apply(this, arguments);\n" +
                    "      \n" +
                    "      // Then check for url changes and script status\n" +
                    "      setTimeout(function() {\n" +
                    "        checkUrlChange();\n" +
                    "        checkRecorderScript();\n" +
                    "      }, 100);\n" +
                    "      \n" +
                    "      return result;\n" +
                    "    };\n" +
                    "    \n" +
                    "    window.history.replaceState = function() {\n" +
                    "      console.debug('CSTestForge: history.replaceState detected');\n" +
                    "      \n" +
                    "      // Call original method first\n" +
                    "      const result = originalReplaceState.apply(this, arguments);\n" +
                    "      \n" +
                    "      // Then check for url changes and script status\n" +
                    "      setTimeout(function() {\n" +
                    "        checkUrlChange();\n" +
                    "        checkRecorderScript();\n" +
                    "      }, 100);\n" +
                    "      \n" +
                    "      return result;\n" +
                    "    };\n" +
                    "    \n" +
                    "    // Monitor popstate event (browser back/forward buttons)\n" +
                    "    window.addEventListener('popstate', function() {\n" +
                    "      console.debug('CSTestForge: popstate event detected');\n" +
                    "      \n" +
                    "      setTimeout(function() {\n" +
                    "        checkUrlChange();\n" +
                    "        checkRecorderScript();\n" +
                    "      }, 100);\n" +
                    "    });\n" +
                    "    \n" +
                    "    // Also check on hash changes for older SPA implementations\n" +
                    "    window.addEventListener('hashchange', function() {\n" +
                    "      console.debug('CSTestForge: hashchange event detected');\n" +
                    "      \n" +
                    "      setTimeout(function() {\n" +
                    "        checkUrlChange();\n" +
                    "        checkRecorderScript();\n" +
                    "      }, 100);\n" +
                    "    });\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Create the main mutation observer\n" +
                    "  window.__csMutationObserver = new MutationObserver(function(mutations) {\n" +
                    "    window.__csLastMutationTime = Date.now();\n" +
                    "    window.__csMutationObserverEventCounts.mutations++;\n" +
                    "    \n" +
                    "    let needsCheck = false;\n" +
                    "    let scriptRemoved = false;\n" +
                    "    let uiRemoved = false;\n" +
                    "    let significantChanges = false;\n" +
                    "    \n" +
                    "    for (const mutation of mutations) {\n" +
                    "      // Check for script removal\n" +
                    "      if (mutation.type === 'childList' && mutation.removedNodes.length > 0) {\n" +
                    "        for (let i = 0; i < mutation.removedNodes.length; i++) {\n" +
                    "          const node = mutation.removedNodes[i];\n" +
                    "          \n" +
                    "          // Safety check - make sure node exists and has necessary properties\n" +
                    "          if (node && node.nodeType) {\n" +
                    "            // Check for direct script removal\n" +
                    "            if (node.id === 'cs-recorder-script' || \n" +
                    "                (node.tagName === 'SCRIPT' && node.textContent && \n" +
                    "                 node.textContent.includes('__csRecorderActive'))) {\n" +
                    "              console.debug('CSTestForge: Recorder script was removed');\n" +
                    "              scriptRemoved = true;\n" +
                    "              needsCheck = true;\n" +
                    "            } else if (node.id === 'cs-recorder-indicator') {\n" +
                    "              console.debug('CSTestForge: Recorder UI was removed');\n" +
                    "              uiRemoved = true;\n" +
                    "              needsCheck = true;\n" +
                    "            }\n" +
                    "            \n" +
                    "            // Also check if removed nodes contain our elements - safely\n" +
                    "            if (node.nodeType === Node.ELEMENT_NODE) {\n" +
                    "              try {\n" +
                    "                if (node.querySelector && (node.querySelector('#cs-recorder-script') || \n" +
                    "                    node.querySelector('#cs-recorder-indicator'))) {\n" +
                    "                  console.debug('CSTestForge: Recorder elements were removed indirectly');\n" +
                    "                  needsCheck = true;\n" +
                    "                }\n" +
                    "              } catch (err) {\n" +
                    "                // Ignore querySelector errors on detached nodes\n" +
                    "                console.debug('CSTestForge: Error checking removed node:', err.message);\n" +
                    "              }\n" +
                    "            }\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }\n" +
                    "      \n" +
                    "      // Check for significant DOM changes that might indicate a SPA navigation\n" +
                    "      if (mutation.type === 'childList' && \n" +
                    "          (mutation.target === document.body || mutation.target === document.documentElement ||\n" +
                    "           (mutation.target.id && \n" +
                    "            (['app', 'root', 'main', 'content', 'page'].includes(mutation.target.id) || \n" +
                    "             mutation.target.id.includes('app') || \n" +
                    "             mutation.target.id.includes('root'))))) {\n" +
                    "        // This is probably a main container - significant changes here are important\n" +
                    "        if (mutation.addedNodes.length > 3 || mutation.removedNodes.length > 3) {\n" +
                    "          significantChanges = true;\n" +
                    "          window.__csMutationObserverEventCounts.domChanges++;\n" +
                    "          needsCheck = true;\n" +
                    "          console.debug('CSTestForge: Significant DOM changes detected');\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (needsCheck) {\n" +
                    "      // First check for URL changes as this often happens during SPA navigation\n" +
                    "      checkUrlChange();\n" +
                    "      \n" +
                    "      // Then verify the recorder script\n" +
                    "      setTimeout(function() {\n" +
                    "        if (scriptRemoved || uiRemoved || significantChanges) {\n" +
                    "          checkRecorderScript();\n" +
                    "        }\n" +
                    "      }, 200);\n" +
                    "    }\n" +
                    "  });\n" +
                    "  \n" +
                    "  // Start observing the entire document\n" +
                    "  window.__csMutationObserver.observe(document, {\n" +
                    "    childList: true,\n" +
                    "    subtree: true,\n" +
                    "    attributes: false,\n" +
                    "    characterData: false\n" +
                    "  });\n" +
                    "  \n" +
                    "  // Setup regular health check interval\n" +
                    "  window.__csHealthCheckInterval = setInterval(function() {\n" +
                    "    // Verify recorder is active during regular health checks\n" +
                    "    checkRecorderScript();\n" +
                    "    \n" +
                    "    // Also check URL, which can sometimes change without triggering events\n" +
                    "    checkUrlChange();\n" +
                    "  }, 10000); // Check every 10 seconds\n" +
                    "  \n" +
                    "  // Set up framework-specific listeners\n" +
                    "  setupFrameworkSpecificListeners();\n" +
                    "  \n" +
                    "  // Set up history API listeners\n" +
                    "  setupHistoryListeners();\n" +
                    "  \n" +
                    "  // Set up handlers for our custom events\n" +
                    "  window.addEventListener('cs_recorder_script_needed', function(e) {\n" +
                    "    console.debug('CSTestForge: cs_recorder_script_needed event received', e.detail);\n" +
                    "  });\n" +
                    "  \n" +
                    "  window.addEventListener('cs_url_changed', function(e) {\n" +
                    "    console.debug('CSTestForge: cs_url_changed event received', e.detail);\n" +
                    "  });\n" +
                    "  \n" +
                    "  window.addEventListener('cs_recorder_ui_needed', function(e) {\n" +
                    "    console.debug('CSTestForge: cs_recorder_ui_needed event received', e.detail);\n" +
                    "    \n" +
                    "    // If we have the UI creation function, try to use it\n" +
                    "    if (typeof window.createRecorderUI === 'function') {\n" +
                    "      try {\n" +
                    "        window.createRecorderUI();\n" +
                    "      } catch(e) {\n" +
                    "        console.error('CSTestForge: Error creating recorder UI:', e);\n" +
                    "      }\n" +
                    "    }\n" +
                    "  });\n" +
                    "  \n" +
                    "  // Set up iframe handling\n" +
                    "  function setupIframeHandling() {\n" +
                    "    // Function to process iframes\n" +
                    "    function processIframes() {\n" +
                    "      const iframes = document.querySelectorAll('iframe');\n" +
                    "      if (iframes.length === 0) return;\n" +
                    "      \n" +
                    "      console.debug('CSTestForge: Processing ' + iframes.length + ' iframes');\n" +
                    "      \n" +
                    "      iframes.forEach(function(iframe) {\n" +
                    "        try {\n" +
                    "          // Skip if iframe is from a different origin (will throw error)\n" +
                    "          if (iframe.contentDocument) {\n" +
                    "            // Check if recorder script is present in iframe\n" +
                    "            const hasRecorder = iframe.contentWindow.__csRecorderActive === true;\n" +
                    "            \n" +
                    "            if (!hasRecorder) {\n" +
                    "              console.debug('CSTestForge: Iframe detected without recorder script');\n" +
                    "              \n" +
                    "              try {\n" +
                    "                window.dispatchEvent(new CustomEvent('cs_iframe_detected', {\n" +
                    "                  detail: { \n" +
                    "                    iframe: iframe,\n" +
                    "                    src: iframe.src,\n" +
                    "                    id: iframe.id,\n" +
                    "                    timestamp: Date.now()\n" +
                    "                  }\n" +
                    "                }));\n" +
                    "              } catch(e) {\n" +
                    "                console.error('CSTestForge: Error dispatching iframe event:', e);\n" +
                    "              }\n" +
                    "            }\n" +
                    "          }\n" +
                    "        } catch (e) {\n" +
                    "          // Cross-origin iframe, can't access\n" +
                    "          console.debug('CSTestForge: Cross-origin iframe detected', iframe.src);\n" +
                    "        }\n" +
                    "      });\n" +
                    "    }\n" +
                    "    \n" +
                    "    // Process iframes initially with delay to ensure page is loaded\n" +
                    "    setTimeout(processIframes, 2000);\n" +
                    "    \n" +
                    "    // Set up observer for new iframes\n" +
                    "    const iframeObserver = new MutationObserver(function(mutations) {\n" +
                    "      let hasNewIframe = false;\n" +
                    "      \n" +
                    "      mutations.forEach(function(mutation) {\n" +
                    "        if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
                    "          for (let i = 0; i < mutation.addedNodes.length; i++) {\n" +
                    "            const node = mutation.addedNodes[i];\n" +
                    "            // Add safety check for node type\n" +
                    "            if (node && (node.nodeName === 'IFRAME' || \n" +
                    "                (node.nodeType === Node.ELEMENT_NODE && node.querySelector && node.querySelector('iframe')))) {\n" +
                    "              hasNewIframe = true;\n" +
                    "              break;\n" +
                    "            }\n" +
                    "          }\n" +
                    "        }\n" +
                    "      });\n" +
                    "      \n" +
                    "      if (hasNewIframe) {\n" +
                    "        console.debug('CSTestForge: New iframe detected');\n" +
                    "        setTimeout(processIframes, 1000); // Wait for iframe to load\n" +
                    "      }\n" +
                    "    });\n" +
                    "    \n" +
                    "    iframeObserver.observe(document, {\n" +
                    "      childList: true,\n" +
                    "      subtree: true,\n" +
                    "      attributes: false\n" +
                    "    });\n" +
                    "    \n" +
                    "    // Also set up a listener for iframe load events\n" +
                    "    document.addEventListener('load', function(e) {\n" +
                    "      if (e.target && e.target.tagName === 'IFRAME') {\n" +
                    "        console.debug('CSTestForge: Iframe loaded', e.target.src);\n" +
                    "        setTimeout(function() {\n" +
                    "          processIframes();\n" +
                    "        }, 500);\n" +
                    "      }\n" +
                    "    }, true);\n" +
                    "  }\n" +
                    "  \n" +
                    "  // Set up iframe handling\n" +
                    "  setupIframeHandling();\n" +
                    "  \n" +
                    "  console.debug('CSTestForge: Advanced mutation observer setup complete');\n" +
                    "  \n" +
                    "  return {\n" +
                    "    status: 'installed',\n" +
                    "    observerType: 'advanced',\n" +
                    "    frameworks: window.__csSpaFrameworks || [],\n" +
                    "    timestamp: Date.now(),\n" +
                    "    url: window.location.href\n" +
                    "  };\n" +
                    "})();";

    /**
     * Install the mutation observer in the browser with enhanced monitoring
     *
     * @param sessionId The recording session ID
     * @return true if the observer was installed successfully
     */
    public boolean installMutationObserver(UUID sessionId) {
        if (sessionId == null) {
            logger.warn("Cannot install mutation observer - sessionId is null");
            return false;
        }

        try {
            // Track installation attempt
            ObserverStatus status = observerStatuses.computeIfAbsent(sessionId, id -> new ObserverStatus());
            status.lastChecked = System.currentTimeMillis();

            // Execute the script
            Object result = browserManager.executeScript(sessionId, MUTATION_OBSERVER_SCRIPT);

            // Parse the result
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;

                String installStatus = (String) resultMap.getOrDefault("status", "unknown");

                if ("installed".equals(installStatus) || "already_installed".equals(installStatus)) {
                    // Successfully installed
                    status.installed = true;
                    status.installTime = System.currentTimeMillis();

                    // Capture current URL if available
                    if (resultMap.containsKey("url")) {
                        status.currentUrl = (String) resultMap.get("url");
                    } else {
                        // Try to get current URL directly
                        String currentUrl = browserManager.getCurrentUrl(sessionId);
                        if (currentUrl != null) {
                            status.currentUrl = currentUrl;
                        }
                    }

                    // Log additional information if available
                    if (resultMap.containsKey("frameworks")) {
                        @SuppressWarnings("unchecked")
                        List<String> frameworks = (List<String>) resultMap.get("frameworks");
                        if (frameworks != null && !frameworks.isEmpty()) {
                            logger.info("Detected SPA frameworks for session {}: {}", sessionId, frameworks);
                        }
                    }

                    logger.info("Successfully installed mutation observer for session: {}", sessionId);
                    return true;
                } else {
                    // Failed to install
                    logger.warn("Failed to install mutation observer for session {}: status={}",
                            sessionId, installStatus);
                    status.retryCount++;
                    return false;
                }
            } else if (result != null) {
                // Some result was returned, but not in expected format
                logger.info("Mutation observer script returned unexpected result type: {}",
                        result.getClass().getName());
                status.installed = true; // Assume it worked
                status.installTime = System.currentTimeMillis();
                return true;
            } else {
                // No result returned
                logger.warn("Failed to install mutation observer for session {}: no result returned",
                        sessionId);
                status.retryCount++;
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to install mutation observer for session {}: {}",
                    sessionId, e.getMessage(), e);

            // Update status
            ObserverStatus status = observerStatuses.get(sessionId);
            if (status != null) {
                status.retryCount++;
            }

            return false;
        }
    }

    /**
     * Perform periodic health checks for all sessions
     */
    private void performHealthChecks() {
        logger.debug("Running mutation observer health checks for {} sessions", observerStatuses.size());

        long now = System.currentTimeMillis();

        // Clone the map to avoid concurrent modification issues
        Map<UUID, ObserverStatus> statuses = new HashMap<>(observerStatuses);

        for (Map.Entry<UUID, ObserverStatus> entry : statuses.entrySet()) {
            UUID sessionId = entry.getKey();
            ObserverStatus status = entry.getValue();

            // Skip sessions that were checked recently
            if (now - status.lastChecked < 10000) { // 10 seconds
                continue;
            }

            // First check if the browser session is still active
            if (!browserManager.isSessionActive(sessionId)) {
                logger.debug("Removing mutation observer status for inactive session: {}", sessionId);
                observerStatuses.remove(sessionId);
                continue;
            }

            // Update last checked time
            status.lastChecked = now;

            // If not installed or it's been a long time since installation, try to install
            if (!status.installed || now - status.installTime > 300000) { // 5 minutes
                logger.debug("Reinstalling mutation observer for session {}", sessionId);

                // Limit retry attempts
                if (status.retryCount < 5) {
                    installMutationObserver(sessionId);
                } else {
                    logger.warn("Giving up on mutation observer installation for session {} after {} retries",
                            sessionId, status.retryCount);
                }
                continue;
            }

            // Check if recorder script is still active
            if (isRecorderScriptPresent(sessionId)) {
                logger.debug("Recorder script is active for session {}", sessionId);
            } else {
                logger.warn("Recorder script not active for session {}, requesting reinjection",
                        sessionId);

                // Attempt script reinjection
                boolean success = browserManager.injectRecorderScriptWithStrategies(sessionId);

                if (success) {
                    logger.info("Successfully reinjected recorder script for session {}", sessionId);
                } else {
                    logger.warn("Failed to reinject recorder script for session {}", sessionId);
                }
            }

            // Check for URL changes that might have been missed
            checkForUrlChanges(sessionId, status);
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

            // Also check for UI indicator
            Object uiPresent = browserManager.executeScript(sessionId,
                    "return document.getElementById('cs-recorder-indicator') !== null;");

            return Boolean.TRUE.equals(result) || Boolean.TRUE.equals(uiPresent);
        } catch (Exception e) {
            logger.warn("Failed to check recorder script for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Check for URL changes that might have been missed by event listeners
     *
     * @param sessionId The session ID
     * @param status The current observer status
     */
    private void checkForUrlChanges(UUID sessionId, ObserverStatus status) {
        try {
            String currentUrl = browserManager.getCurrentUrl(sessionId);

            if (currentUrl != null && !currentUrl.equals(status.currentUrl)) {
                logger.info("Detected URL change for session {}: {} -> {}",
                        sessionId, status.currentUrl, currentUrl);

                // Update stored URL
                status.currentUrl = currentUrl;

                // Handle the SPA navigation
                handleSpaNavigation(sessionId, currentUrl);
            }
        } catch (Exception e) {
            logger.warn("Error checking for URL changes for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Handle a SPA navigation event with robust error handling
     *
     * @param sessionId The session ID
     * @param url The new URL
     */
    public void handleSpaNavigation(UUID sessionId, String url) {
        if (sessionId == null) {
            return;
        }

        logger.info("SPA navigation detected for session {}: {}", sessionId, url);

        try {
            // First check if browser is still responsive
            if (!browserManager.isSessionActive(sessionId)) {
                logger.warn("Cannot handle SPA navigation - browser session is inactive");
                return;
            }

            // Re-inject the recorder script
            boolean scriptInjected = browserManager.injectRecorderScriptWithStrategies(sessionId);

            if (scriptInjected) {
                logger.info("Successfully reinjected recorder script after SPA navigation");
            } else {
                logger.warn("Failed to reinject recorder script after SPA navigation");

                // Try alternative approaches if main strategy failed
                boolean altInjection = browserManager.injectUltraCompatibleScript(sessionId);

                if (altInjection) {
                    logger.info("Successfully reinjected alternate script after SPA navigation");
                } else {
                    logger.warn("All script injection strategies failed after SPA navigation");
                }
            }

            // Re-install the mutation observer
            boolean observerInstalled = installMutationObserver(sessionId);

            if (observerInstalled) {
                logger.info("Successfully reinstalled mutation observer after SPA navigation");
            } else {
                logger.warn("Failed to reinstall mutation observer after SPA navigation");
            }
        } catch (Exception e) {
            logger.error("Error handling SPA navigation for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Handle an iframe detection event
     *
     * @param sessionId The session ID
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
                    "if (!iframe || !iframe.contentWindow) return { error: 'Iframe not found' };\n" +
                    "try {\n" +
                    "  // Check if we can access the iframe (same-origin policy)\n" +
                    "  iframe.contentDocument;\n" +
                    "  return { canAccess: true };\n" +
                    "} catch (e) {\n" +
                    "  console.error('Cannot access iframe: ' + e.message);\n" +
                    "  return { canAccess: false, error: e.message };\n" +
                    "}";

            Object accessResult = browserManager.executeScript(sessionId, script);

            boolean canAccess = false;
            if (accessResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> accessMap = (Map<String, Object>) accessResult;
                canAccess = Boolean.TRUE.equals(accessMap.get("canAccess"));
            }

            if (!canAccess) {
                logger.warn("Cannot access iframe for session {}: cross-origin restriction", sessionId);
                return false;
            }

            // For same-origin iframes, we can inject our scripts
            boolean scriptInjected = injectScriptIntoIframe(sessionId, iframeSelector);
            boolean observerInjected = injectMutationObserverIntoIframe(sessionId, iframeSelector);

            if (scriptInjected) {
                logger.info("Successfully injected recorder script into iframe for session: {}", sessionId);
            } else {
                logger.warn("Failed to inject recorder script into iframe");
            }

            if (observerInjected) {
                logger.info("Successfully injected mutation observer into iframe for session: {}", sessionId);
            } else {
                logger.warn("Failed to inject mutation observer into iframe");
            }

            return scriptInjected || observerInjected;
        } catch (Exception e) {
            logger.error("Failed to handle iframe detection for session {}: {}",
                    sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Inject the recorder script into an iframe
     *
     * @param sessionId The session ID
     * @param iframeSelector CSS selector for the iframe
     * @return true if successful
     */
    private boolean injectScriptIntoIframe(UUID sessionId, String iframeSelector) {
        try {
            // Get the server host and port for absolute URLs
            String serverHost = System.getProperty("server.address");
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = System.getProperty("server.host", "localhost");
            }
            String serverPort = System.getProperty("server.port", "8080");
            String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            // Create script to inject into iframe
            String injectScript =
                    "const iframe = document.querySelector('" + iframeSelector + "');\n" +
                            "if (!iframe || !iframe.contentWindow) return { error: 'Iframe not found' };\n" +
                            "\n" +
                            "try {\n" +
                            "  const doc = iframe.contentDocument;\n" +
                            "  if (!doc) return { error: 'Cannot access iframe document' };\n" +
                            "  \n" +
                            "  // Check if script is already present\n" +
                            "  if (iframe.contentWindow.__csRecorderActive === true) {\n" +
                            "    return { status: 'already_injected' };\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Create minimal recorder script for the iframe\n" +
                            "  const script = doc.createElement('script');\n" +
                            "  script.id = 'cs-recorder-script';\n" +
                            "  script.textContent = `\n" +
                            "    (function() {\n" +
                            "      window.__csRecorderActive = true;\n" +
                            "      window.__csRecorderSessionId = '" + sessionId.toString() + "';\n" +
                            "      window.__csServerHost = '" + serverHost + "';\n" +
                            "      window.__csServerPort = '" + serverPort + "';\n" +
                            "      window.__csContextPath = '" + contextPath + "';\n" +
                            "      window.__csApiEndpoint = 'http://" + serverHost + ":" + serverPort +
                            contextPath + "/api/recorder/events/" + sessionId.toString() + "';\n" +
                            "      console.debug('CSTestForge: Iframe recorder initialized');\n" +
                            "      \n" +
                            "      // Send events via parent window if possible\n" +
                            "      window.__csSendEvent = function(eventData) {\n" +
                            "        try {\n" +
                            "          // First try to use parent window's send function\n" +
                            "          if (window.parent && window.parent.__csSendEvent) {\n" +
                            "            eventData.fromIframe = true;\n" +
                            "            window.parent.__csSendEvent(eventData);\n" +
                            "            return true;\n" +
                            "          }\n" +
                            "          \n" +
                            "          // Fall back to direct XHR\n" +
                            "          const xhr = new XMLHttpRequest();\n" +
                            "          xhr.open('POST', window.__csApiEndpoint, true);\n" +
                            "          xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "          xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "          eventData.sessionId = '" + sessionId.toString() + "';\n" +
                            "          eventData.fromIframe = true;\n" +
                            "          xhr.send(JSON.stringify(eventData));\n" +
                            "          return true;\n" +
                            "        } catch(e) {\n" +
                            "          console.error('Failed to send event from iframe:', e);\n" +
                            "          return false;\n" +
                            "        }\n" +
                            "      };\n" +
                            "      \n" +
                            "      // Track clicks\n" +
                            "      document.addEventListener('click', function(e) {\n" +
                            "        __csSendEvent({\n" +
                            "          type: 'CLICK',\n" +
                            "          url: window.location.href,\n" +
                            "          title: document.title,\n" +
                            "          timestamp: new Date().getTime(),\n" +
                            "          elementInfo: {\n" +
                            "            tagName: e.target.tagName.toLowerCase(),\n" +
                            "            id: e.target.id || null,\n" +
                            "            className: e.target.className || null,\n" +
                            "            text: e.target.textContent ? e.target.textContent.trim().substring(0, 100) : null\n" +
                            "          }\n" +
                            "        });\n" +
                            "      }, true);\n" +
                            "      \n" +
                            "      // Track input changes\n" +
                            "      document.addEventListener('input', function(e) {\n" +
                            "        if (!e.target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName)) return;\n" +
                            "        __csSendEvent({\n" +
                            "          type: 'INPUT',\n" +
                            "          url: window.location.href,\n" +
                            "          title: document.title,\n" +
                            "          timestamp: new Date().getTime(),\n" +
                            "          value: e.target.type === 'password' ? '********' : e.target.value,\n" +
                            "          elementInfo: {\n" +
                            "            tagName: e.target.tagName.toLowerCase(),\n" +
                            "            id: e.target.id || null,\n" +
                            "            name: e.target.name || null,\n" +
                            "            type: e.target.type || null\n" +
                            "          }\n" +
                            "        });\n" +
                            "      }, true);\n" +
                            "      \n" +
                            "      // Send init event\n" +
                            "      __csSendEvent({\n" +
                            "        type: 'INIT',\n" +
                            "        url: window.location.href,\n" +
                            "        title: document.title,\n" +
                            "        timestamp: new Date().getTime(),\n" +
                            "        userAgent: navigator.userAgent,\n" +
                            "        fromIframe: true\n" +
                            "      });\n" +
                            "      \n" +
                            "      console.debug('CSTestForge: Iframe recorder setup complete');\n" +
                            "    })();\n" +
                            "  `;\n" +
                            "  \n" +
                            "  // Add script to iframe document\n" +
                            "  (doc.head || doc.documentElement).appendChild(script);\n" +
                            "  \n" +
                            "  return { status: 'injected' };\n" +
                            "} catch(e) {\n" +
                            "  console.error('CSTestForge: Error injecting script into iframe:', e);\n" +
                            "  return { error: e.toString() };\n" +
                            "}";

            Object result = browserManager.executeScript(sessionId, injectScript);

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;

                if (resultMap.containsKey("status")) {
                    String status = (String) resultMap.get("status");
                    return "injected".equals(status) || "already_injected".equals(status);
                } else if (resultMap.containsKey("error")) {
                    logger.warn("Failed to inject script into iframe: {}", resultMap.get("error"));
                    return false;
                }
            }

            // Default to true if we got a result that wasn't an error
            return result != null;
        } catch (Exception e) {
            logger.error("Error injecting script into iframe: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Inject a simplified mutation observer into an iframe
     *
     * @param sessionId The session ID
     * @param iframeSelector CSS selector for the iframe
     * @return true if successful
     */
    private boolean injectMutationObserverIntoIframe(UUID sessionId, String iframeSelector) {
        try {
            // Create a simplified observer for iframes
            String observerScript =
                    "const iframe = document.querySelector('" + iframeSelector + "');\n" +
                            "if (!iframe || !iframe.contentWindow) return { error: 'Iframe not found' };\n" +
                            "\n" +
                            "try {\n" +
                            "  const doc = iframe.contentDocument;\n" +
                            "  if (!doc) return { error: 'Cannot access iframe document' };\n" +
                            "  \n" +
                            "  // Check if observer is already present\n" +
                            "  if (iframe.contentWindow.__csMutationObserver) {\n" +
                            "    return { status: 'already_injected' };\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Create script element\n" +
                            "  const script = doc.createElement('script');\n" +
                            "  script.id = 'cs-mutation-observer';\n" +
                            "  script.textContent = `\n" +
                            "    (function() {\n" +
                            "      // Don't install multiple observers\n" +
                            "      if (window.__csMutationObserver) return;\n" +
                            "      \n" +
                            "      console.debug('CSTestForge: Setting up iframe mutation observer');\n" +
                            "      \n" +
                            "      // Create mutation observer\n" +
                            "      window.__csMutationObserver = new MutationObserver(function(mutations) {\n" +
                            "        let needsCheck = false;\n" +
                            "        \n" +
                            "        for (const mutation of mutations) {\n" +
                            "          if (mutation.type === 'childList' && \n" +
                            "              (mutation.addedNodes.length > 2 || mutation.removedNodes.length > 2)) {\n" +
                            "            needsCheck = true;\n" +
                            "            break;\n" +
                            "          }\n" +
                            "        }\n" +
                            "        \n" +
                            "        if (needsCheck) {\n" +
                            "          setTimeout(function() {\n" +
                            "            // Check if recorder is still active\n" +
                            "            if (!window.__csRecorderActive) {\n" +
                            "              console.debug('CSTestForge: Iframe recorder not active after DOM changes');\n" +
                            "              \n" +
                            "              // Try to signal parent window\n" +
                            "              try {\n" +
                            "                if (window.parent) {\n" +
                            "                  window.parent.postMessage({\n" +
                            "                    type: 'cs_recorder_needed',\n" +
                            "                    source: 'iframe',\n" +
                            "                    url: window.location.href,\n" +
                            "                    selector: '" + iframeSelector + "'\n" +
                            "                  }, '*');\n" +
                            "                }\n" +
                            "              } catch(e) {\n" +
                            "                console.error('Error signaling parent:', e);\n" +
                            "              }\n" +
                            "            }\n" +
                            "          }, 100);\n" +
                            "        }\n" +
                            "      });\n" +
                            "      \n" +
                            "      // Start observing\n" +
                            "      window.__csMutationObserver.observe(document, {\n" +
                            "        childList: true,\n" +
                            "        subtree: true,\n" +
                            "        attributes: false\n" +
                            "      });\n" +
                            "      \n" +
                            "      // Handle messages from parent window\n" +
                            "      window.addEventListener('message', function(event) {\n" +
                            "        if (event.data && event.data.type === 'cs_recorder_check') {\n" +
                            "          event.source.postMessage({\n" +
                            "            type: 'cs_recorder_status',\n" +
                            "            active: window.__csRecorderActive === true,\n" +
                            "            url: window.location.href\n" +
                            "          }, '*');\n" +
                            "        }\n" +
                            "      });\n" +
                            "      \n" +
                            "      console.debug('CSTestForge: Iframe mutation observer setup complete');\n" +
                            "    })();\n" +
                            "  `;\n" +
                            "  \n" +
                            "  // Add script to iframe document\n" +
                            "  (doc.head || doc.documentElement).appendChild(script);\n" +
                            "  \n" +
                            "  // Add a message listener to the parent window to receive messages from the iframe\n" +
                            "  if (!window.__csIframeMessageListenerInstalled) {\n" +
                            "    window.addEventListener('message', function(event) {\n" +
                            "      if (event.data && event.data.type === 'cs_recorder_needed') {\n" +
                            "        console.debug('CSTestForge: Received recorder needed message from iframe');\n" +
                            "        \n" +
                            "        // Signal need for script reinjection\n" +
                            "        window.dispatchEvent(new CustomEvent('cs_iframe_needs_script', {\n" +
                            "          detail: event.data\n" +
                            "        }));\n" +
                            "      }\n" +
                            "    });\n" +
                            "    \n" +
                            "    window.__csIframeMessageListenerInstalled = true;\n" +
                            "  }\n" +
                            "  \n" +
                            "  return { status: 'injected' };\n" +
                            "} catch(e) {\n" +
                            "  console.error('CSTestForge: Error injecting mutation observer into iframe:', e);\n" +
                            "  return { error: e.toString() };\n" +
                            "}";

            Object result = browserManager.executeScript(sessionId, observerScript);

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;

                if (resultMap.containsKey("status")) {
                    String status = (String) resultMap.get("status");
                    return "injected".equals(status) || "already_injected".equals(status);
                } else if (resultMap.containsKey("error")) {
                    logger.warn("Failed to inject mutation observer into iframe: {}", resultMap.get("error"));
                    return false;
                }
            }

            // Default to true if we got a result that wasn't an error
            return result != null;
        } catch (Exception e) {
            logger.error("Error injecting mutation observer into iframe: {}", e.getMessage());
            return false;
        }
    }
}