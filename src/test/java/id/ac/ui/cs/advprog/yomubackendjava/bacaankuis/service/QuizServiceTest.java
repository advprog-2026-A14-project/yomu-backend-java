package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private UserAttemptRepository attemptRepository;

    @InjectMocks
    private QuizService quizService;

    @Test
    void testCompleteQuiz_AlreadyDone_ThrowsException() {
        UUID userId = UUID.randomUUID();
        String kuisId = "kuis-123";

        QuizSyncRequest request = new QuizSyncRequest(userId, kuisId, 100.0, 100.0);

        when(attemptRepository.existsByUserIdAndKuisId(userId, kuisId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            quizService.submitAndSync(request);
        });
    }
}