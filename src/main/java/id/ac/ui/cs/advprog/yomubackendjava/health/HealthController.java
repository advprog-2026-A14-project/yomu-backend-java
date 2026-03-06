package id.ac.ui.cs.advprog.yomubackendjava.health;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    public static final String HEALTH_PATH = "/health";

    @GetMapping(HEALTH_PATH)
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success("Health check successful", Map.of("status", "ok")));
    }
}
