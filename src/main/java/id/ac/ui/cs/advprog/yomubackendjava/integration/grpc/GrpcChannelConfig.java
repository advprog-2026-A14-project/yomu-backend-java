package id.ac.ui.cs.advprog.yomubackendjava.integration.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "rust.integration.transport", havingValue = "grpc", matchIfMissing = true)
public class GrpcChannelConfig {
    private static final String API_KEY_HEADER = "x-api-key";

    @Value("${internal.api.key:}")
    private String apiKey;

    @Value("${rust.grpc.deadline-ms:3000}")
    private long deadlineMillis;

    @Bean
    public ManagedChannel rustEngineChannel(GrpcChannelFactory channelFactory) {
        requireApiKey();
        return channelFactory.createChannel("rust-engine");
    }

    @Bean
    public Metadata rustEngineMetadata() {
        requireApiKey();
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of(API_KEY_HEADER, Metadata.ASCII_STRING_MARSHALLER), apiKey);
        return metadata;
    }

    @Bean
    public Duration rustGrpcDeadline() {
        return Duration.ofMillis(deadlineMillis);
    }

    @Bean
    @ConditionalOnMissingBean(RustEngineClient.class)
    public RustEngineClient grpcUserSyncClient(
            ManagedChannel rustEngineChannel,
            Metadata rustEngineMetadata,
            Duration rustGrpcDeadline
    ) {
        return new GrpcUserSyncClient(rustEngineChannel, rustEngineMetadata, rustGrpcDeadline);
    }

    @Bean
    @ConditionalOnMissingBean(QuizSyncClient.class)
    public QuizSyncClient grpcQuizSyncClient(
            ManagedChannel rustEngineChannel,
            Metadata rustEngineMetadata,
            Duration rustGrpcDeadline
    ) {
        return new GrpcQuizSyncClient(rustEngineChannel, rustEngineMetadata, rustGrpcDeadline);
    }

    @Bean
    @ConditionalOnMissingBean(RustLeagueClient.class)
    public RustLeagueClient grpcLeagueClient(
            ManagedChannel rustEngineChannel,
            Metadata rustEngineMetadata,
            Duration rustGrpcDeadline
    ) {
        return new GrpcLeagueClient(rustEngineChannel, rustEngineMetadata, rustGrpcDeadline);
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("internal.api.key must be configured for Rust gRPC integration");
        }
    }
}
