package id.ac.ui.cs.advprog.yomubackendjava.integration.rust;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RustLeagueClientTest {

    private MockRestServiceServer server;
    private RestClientRustLeagueClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientRustLeagueClient(builder.build());
    }

    @Test
    void getUserTier_userHasClan_shouldReturnTierResponse() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo("http://localhost:8080/api/v1/league/users/" + userId + "/tier"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "message": "Data liga pengguna berhasil diambil",
                          "data": {
                            "user_id": "%s",
                            "clan_id": "abc12345-0000-0000-0000-000000000000",
                            "clan_name": "Sipaling Coding",
                            "tier": "Diamond"
                          }
                        }
                        """.formatted(userId), MediaType.APPLICATION_JSON));

        RustLeagueClient.UserTierResponse response = client.getUserTier(userId);

        assertNotNull(response);
        assertEquals("Diamond", response.tier());
        assertEquals("Sipaling Coding", response.clanName());
        server.verify();
    }

    @Test
    void getUserTier_rustDown_shouldThrowException() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo("http://localhost:8080/api/v1/league/users/" + userId + "/tier"))
                .andRespond(withServerError());

        assertThrows(Exception.class, () -> client.getUserTier(userId));
        server.verify();
    }
}