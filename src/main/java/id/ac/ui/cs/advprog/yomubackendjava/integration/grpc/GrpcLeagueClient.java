package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.LeagueServiceGrpc;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.GetUserTierRequest;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.GetLeaderboardRequest;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.LeaderboardEntry;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.JoinClanRequest;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

import java.util.UUID;

public class GrpcLeagueClient implements RustLeagueClient {

    private final LeagueServiceGrpc.LeagueServiceBlockingStub stub;

    public GrpcLeagueClient(ManagedChannel rustEngineChannel) {
        this.stub = LeagueServiceGrpc.newBlockingStub(rustEngineChannel);
    }

    @Override
    public UserTierResponse getUserTier(UUID userId) {
        try {
            var request = GetUserTierRequest.newBuilder()
                    .setUserId(userId.toString())
                    .build();
            var response = stub.getUserTier(request);

            UUID clanId = null;
            if (!response.getClanId().isEmpty()) {
                clanId = UUID.fromString(response.getClanId());
            }

            return new UserTierResponse(
                    userId,
                    clanId,
                    response.getClanName(),
                    response.getTier()
            );
        } catch (StatusRuntimeException e) {
            return new UserTierResponse(userId, null, null, null);
        }
    }
}
