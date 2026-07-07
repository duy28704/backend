package com.example.doan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class TurnstileService {

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    @Value("${cloudflare.turnstile.site-key}")
    private String siteKey;

    @Value("${cloudflare.turnstile.verify-url}")
    private String verifyUrl;

    private final RestTemplate restTemplate;

    public TurnstileService() {
        this.restTemplate = new RestTemplate();
    }

    public String getSiteKey() {
        return siteKey;
    }

    /**
     * Verify Turnstile token with Cloudflare API
     * @param token Turnstile response token from frontend widget
     * @return boolean true if validation succeeded
     */
    public boolean verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Turnstile validation failed: token is null or empty");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", secretKey);
            map.add("response", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(verifyUrl, request, Map.class);
            Map<String, Object> body = responseEntity.getBody();

            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                log.info("Cloudflare Turnstile token verified successfully.");
                return true;
            } else {
                log.warn("Cloudflare Turnstile verification failed. Response: {}", body);
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying Turnstile token with Cloudflare API: {}", e.getMessage(), e);
            return false;
        }
    }
}
