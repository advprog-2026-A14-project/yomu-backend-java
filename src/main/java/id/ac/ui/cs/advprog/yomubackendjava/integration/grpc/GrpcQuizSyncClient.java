package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.proto.quizsync.QuizSyncServiceGrpc;
import id.ac.ui.cs.advprog.yomubackendjava.proto.quizsync.SyncQuizHistoryRequest;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class GrpcQuizSyncClient implements QuizSyncClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcQuizSyncClient.class);

    private final QuizSyncServiceGrpc.QuizSyncServiceBlockingStub stub;
    private final Duration deadline;

    public GrpcQuizSyncClient(ManagedChannel rustEngineChannel, Metadata metadata, Duration deadline) {
        this.stub = QuizSyncServiceGrpc.newBlockingStub(rustEngineChannel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
        this.deadline = deadline;
    }

    @Override
    public void sync(QuizSyncRequest request) {
        try {
            var grpcRequest = SyncQuizHistoryRequest.newBuilder()
                    .setUserId(request.getUserId().toString())
                    .setArticleId(request.getArticleId())
                    .setScore(request.getScore())
                    .setAccuracy(request.getAccuracy())
                    .build();
            stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .syncQuizHistory(grpcRequest);
        } catch (StatusRuntimeException ex) {
            LOGGER.warn("Rust quiz gRPC sync failed status={}", ex.getStatus().getCode());
            throw ex;
        }
    }
}
