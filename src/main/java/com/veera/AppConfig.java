package com.veera;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String tokenUrl;
    private String contactIdUrl;
    private String bucketName;
    private String csvFilePath;
}