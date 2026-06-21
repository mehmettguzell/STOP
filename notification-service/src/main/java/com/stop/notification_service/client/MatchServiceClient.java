package com.stop.notification_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MatchServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MatchServiceClient(
            RestTemplate restTemplate,
            @Value("${services.match-service.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<UUID> getParticipantIds(UUID matchId) {
        try {
            String url = baseUrl + "/participation/matches/" + matchId + "/participants/ids";
            List<String> ids = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
            if (ids == null) return Collections.emptyList();
            return ids.stream().map(UUID::fromString).toList();
        } catch (Exception e) {
            log.error("Failed to fetch participants for matchId={}", matchId, e);
            return Collections.emptyList();
        }
    }
}
