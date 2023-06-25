package com.veera;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws")
@Setter
@Getter
public class AWSConfig {
    private String accessKey;
    private String secretKey;
    private String region;
}