package com.stop.api_gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private final Jwt jwt = new Jwt();
    private final RateLimit rateLimit = new RateLimit();
    private final Services services = new Services();

    @Data
    public static class Jwt {
        private String publicKeyLocation = "classpath:keys/public.pem";
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private long capacity = 200;
        private Duration refillPeriod = Duration.ofMinutes(1);
    }

    @Data
    public static class Services {
        private String auth = "http://localhost:8081";
        private String identity = "http://localhost:8081";
        private String trustAndRank =  "http://localhost:8081";
        private String friendship = "http://localhost:8081";
        private String moderation = "http://localhost:8081";

        private String match = "http://localhost:8082";
        private String participation = "http://localhost:8082";
        private String invitation = "http://localhost:8082";

        private String notification = "http://localhost:8083";
        private String communication = "http://localhost:8084";
    }
}
