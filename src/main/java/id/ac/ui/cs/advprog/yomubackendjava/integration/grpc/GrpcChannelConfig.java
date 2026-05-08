import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import id.ac.ui.cs.advprog.yomubackendjava.integration.grpc.GrpcUserSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.grpc.GrpcQuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.grpc.GrpcLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelConfig {

    @Value("${internal.api.key:}")
    private String apiKey;

    @Bean
    public ManagedChannel rustEngineChannel(GrpcChannelFactory channelFactory) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER), apiKey);

        return channelFactory.createChannel("rust-engine");
    }

    @Bean
    @Primary
    public RustEngineClient grpcUserSyncClient(ManagedChannel rustEngineChannel) {
        return new GrpcUserSyncClient(rustEngineChannel);
    }

    @Bean
    @Primary
    public QuizSyncClient grpcQuizSyncClient(ManagedChannel rustEngineChannel) {
        return new GrpcQuizSyncClient(rustEngineChannel);
    }

    @Bean
    @Primary
    public RustLeagueClient grpcLeagueClient(ManagedChannel rustEngineChannel) {
        return new GrpcLeagueClient(rustEngineChannel);
    }
}
