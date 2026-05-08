package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.proto.quizsync.QuizSyncServiceGrpc;
import id.ac.ui.cs.advprog.yomubackendjava.proto.quizsync.SyncQuizHistoryRequest;
import io.grpc.ManagedChannel;
import org.springframework.stereotype.Component;

public class GrpcQuizSyncClient implements QuizSyncClient {

    private final QuizSyncServiceGrpc.QuizSyncServiceBlockingStub stub;

    public GrpcQuizSyncClient(ManagedChannel rustEngineChannel) {
        this.stub = QuizSyncServiceGrpc.newBlockingStub(rustEngineChannel);
    }

    @Override
    public void sync(QuizSyncRequest request) {
        var grpcRequest = SyncQuizHistoryRequest.newBuilder()
                .setUserId(request.getUserId().toString())
                .setArticleId(request.getArticleId())
                .setScore(request.getScore())
                .setAccuracy(request.getAccuracy())
                .build();
        stub.syncQuizHistory(grpcRequest);
    }
}
