package com.motorrent.motorbikerental.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {
    
    @Value("${payos.client-id:YOUR_PAYOS_CLIENT_ID}")
    private String clientId;

    @Value("${payos.api-key:YOUR_PAYOS_API_KEY}")
    private String apiKey;

    @Value("${payos.checksum-key:YOUR_PAYOS_CHECKSUM_KEY}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
