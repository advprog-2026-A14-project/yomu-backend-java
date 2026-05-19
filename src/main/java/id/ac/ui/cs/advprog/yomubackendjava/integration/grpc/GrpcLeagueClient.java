package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.LeagueServiceGrpc;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.GetUserTierRequest;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.GetLeaderboardRequest;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.LeaderboardEntry;
import id.ac.ui.cs.advprog.yomubackendjava.proto.league.JoinClanRequest;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GrpcLeagueClient implements RustLeagueClient {

    private final LeagueServiceGrpc.LeagueServiceBlockingStub stub;
    private final Duration deadline;

    public GrpcLeagueClient(ManagedChannel rustEngineChannel, Metadata metadata, Duration deadline) {
        this.stub = LeagueServiceGrpc.newBlockingStub(rustEngineChannel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
        this.deadline = deadline;
    }

    @Override
    public UserTierResponse getUserTier(UUID userId) {
        try {
            var request = GetUserTierRequest.newBuilder()
                    .setUserId(userId.toString())
                    .build();
            var response = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .getUserTier(request);

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
