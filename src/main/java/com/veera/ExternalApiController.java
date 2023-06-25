package com.veera;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalApiController {


    @Autowired
    private ExternalApiService externalApiService;

    @GetMapping("/call")
    public String callApiAndProcess() {
        externalApiService.processApi();
        return "Success";
    }
}