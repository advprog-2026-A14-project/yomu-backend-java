package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.proto.usersync.UserSyncServiceGrpc;
import id.ac.ui.cs.advprog.yomubackendjava.proto.usersync.SyncShadowUserRequest;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

import java.util.UUID;

public class GrpcUserSyncClient implements RustEngineClient {

    private final UserSyncServiceGrpc.UserSyncServiceBlockingStub stub;

    public GrpcUserSyncClient(ManagedChannel rustEngineChannel) {
        this.stub = UserSyncServiceGrpc.newBlockingStub(rustEngineChannel);
    }

    @Override
    public SyncResult syncUser(UUID userId) {
        try {
            var request = SyncShadowUserRequest.newBuilder()
                    .setUserId(userId.toString())
                    .build();
            var response = stub.syncShadowUser(request);
            return new SyncResult(201, response.getMessage());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.ALREADY_EXISTS) {
                return new SyncResult(409, e.getStatus().getDescription());
            }
            return new SyncResult(500, e.getStatus().getDescription());
        }
    }
}
