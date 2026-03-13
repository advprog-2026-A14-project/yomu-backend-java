package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuizServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private QuizService quizService;

    @Test
    void testJavaStaysAliveIfRustIsDown() {
        QuizSyncRequest request = new QuizSyncRequest("user-1", "art-1", 100, 90.0);

        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Rust Offline"));

        assertDoesNotThrow(() -> {
            quizService.submitAndSync(request);
        });
    }
}