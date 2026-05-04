package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private UserAttemptRepository attemptRepository;

    @InjectMocks
    private QuizService quizService;

    @Test
    void submitAndSync_whenAlreadyCompleted_throwsConflictAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        String articleId = "article-123";
        QuizSyncRequest request = new QuizSyncRequest(userId, articleId, 100.0, 100.0);

        when(attemptRepository.existsByUserIdAndKuisId(userId, articleId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenFirstAttempt_savesUserAttempt() {
        UUID userId = UUID.randomUUID();
        String articleId = "article-123";
        QuizSyncRequest request = new QuizSyncRequest(userId, articleId, 85.0, 90.0);

        when(attemptRepository.existsByUserIdAndKuisId(userId, articleId)).thenReturn(false);

        quizService.submitAndSync(request);

        ArgumentCaptor<UserAttempt> captor = ArgumentCaptor.forClass(UserAttempt.class);
        verify(attemptRepository).save(captor.capture());

        UserAttempt savedAttempt = captor.getValue();
        assertEquals(userId, savedAttempt.getUserId());
        assertEquals(articleId, savedAttempt.getKuisId());
        assertNotNull(savedAttempt.getCompletedAt());
    }

    @Test
    void submitAndSync_whenUserIdMissing_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(null, "article-123", 80.0, 90.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenArticleIdMissing_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(UUID.randomUUID(), "", 80.0, 90.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenScoreIsNegative_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(UUID.randomUUID(), "article-123", -1.0, 90.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenScoreAbove100_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(UUID.randomUUID(), "article-123", 101.0, 90.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenAccuracyIsNegative_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(UUID.randomUUID(), "article-123", 80.0, -1.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }

    @Test
    void submitAndSync_whenAccuracyAbove100_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(UUID.randomUUID(), "article-123", 80.0, 101.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(request));

        verify(attemptRepository, never()).save(any(UserAttempt.class));
    }
}
