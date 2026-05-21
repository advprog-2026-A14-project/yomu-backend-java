package id.ac.ui.cs.advprog.yomubackendjava.health;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    public static final String HEALTH_PATH = "/health";

    private final ManagedChannel rustEngineChannel;

    public HealthController(ManagedChannel rustEngineChannel) {
        this.rustEngineChannel = rustEngineChannel;
    }

    @GetMapping(HEALTH_PATH)
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        String grpcStatus = "disconnected";
        try {
            var connected = rustEngineChannel.getState(true).equals(io.grpc.ConnectivityState.READY);
            grpcStatus = connected ? "connected" : "connecting";
        } catch (StatusRuntimeException e) {
            grpcStatus = "error: " + e.getStatus().getCode();
        }

        return ResponseEntity.ok(ApiResponse.success(
            "Health check successful",
            Map.of("status", "ok", "grpc", grpcStatus)
        ));
    }
}
