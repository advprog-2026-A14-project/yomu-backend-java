package id.ac.ui.cs.advprog.yomubackendjava.integration.rust;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public interface RustLeagueClient {
    UserTierResponse getUserTier(UUID userId);

    record UserTierResponse(
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("clan_id") UUID clanId,
            @JsonProperty("clan_name") String clanName,
            String tier
    ) {}

    record ApiWrapper(
            boolean success,
            String message,
            UserTierResponse data
    ) {}
}