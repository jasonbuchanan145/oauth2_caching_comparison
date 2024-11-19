package edu.usd.jbuchanan.oauth.cache.comparison.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
}