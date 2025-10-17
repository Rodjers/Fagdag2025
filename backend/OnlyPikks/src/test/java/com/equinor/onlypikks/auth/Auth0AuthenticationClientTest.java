package com.equinor.onlypikks.auth;

import com.equinor.onlypikks.api.model.AuthTokensResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class Auth0AuthenticationClientTest {

    @Test
    void loginIncludesRealmWhenConnectionConfigured() {
        Auth0Properties properties = new Auth0Properties(
                "example.auth0.com",
                "https://api.example.com",
                "client-id",
                "client-secret",
                "My-Connection",
                "openid profile",
                false
        );

        Auth0AuthenticationClient client = new Auth0AuthenticationClient(
                properties,
                new RestTemplateBuilder(),
                new ObjectMapper()
        );

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("https://example.auth0.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{" +
                        "\"grant_type\":\"password\"," +
                        "\"username\":\"user@example.com\"," +
                        "\"password\":\"secret\"," +
                        "\"audience\":\"https://api.example.com\"," +
                        "\"client_id\":\"client-id\"," +
                        "\"client_secret\":\"client-secret\"," +
                        "\"scope\":\"openid profile\"," +
                        "\"realm\":\"My-Connection\"" +
                        "}"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{" +
                                "\"access_token\":\"token\"," +
                                "\"refresh_token\":\"refresh\"," +
                                "\"token_type\":\"Bearer\"," +
                                "\"expires_in\":3600" +
                                "}",
                        MediaType.APPLICATION_JSON
                ));

        AuthTokensResponse tokens = client.login("user@example.com", "secret");

        assertThat(tokens.accessToken()).isEqualTo("token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh");
        server.verify();
    }
}
