package com.cstestforge.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SPAController {

    @GetMapping(value = {
            "/cstestforge",
            "/cstestforge/",
            "/cstestforge/dashboard",
            "/cstestforge/projects/**",
            "/cstestforge/recorder/**",
            "/cstestforge/builder/**",
            "/cstestforge/execution/**",
            "/cstestforge/runner/**",
            "/cstestforge/api-testing/**",
            "/cstestforge/export/**",
            "/cstestforge/ado-integration/**",
            "/cstestforge/reports/**",
            "/cstestforge/notifications/**",
            "/cstestforge/settings/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}