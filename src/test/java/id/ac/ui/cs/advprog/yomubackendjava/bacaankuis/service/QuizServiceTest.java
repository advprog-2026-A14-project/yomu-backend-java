package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
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
    private static final String ARTICLE_ID = "article-123";

    @Mock
    private UserAttemptRepository attemptRepository;

    @Mock
    private QuizSyncClient quizSyncClient;

    @InjectMocks
    private QuizService quizService;

    @Test
    void submitAndSync_whenAlreadyCompleted_throwsConflictAndDoesNotSave() {
        UUID userId = UUID.randomUUID();
        QuizSyncRequest request = new QuizSyncRequest(userId, ARTICLE_ID, 100.0, 100.0);

        when(attemptRepository.existsByUserIdAndKuisId(userId, ARTICLE_ID)).thenReturn(true);

        assertThrows(ConflictException.class, () -> quizService.submitAndSync(userId, request));

        verify(attemptRepository, never()).saveAndFlush(any(UserAttempt.class));
        verify(quizSyncClient, never()).sync(any());
    }

    @Test
    void submitAndSync_whenFirstAttempt_savesUserAttemptAndSyncsToRust() {
        UUID userId = UUID.randomUUID();
        QuizSyncRequest request = new QuizSyncRequest(userId, ARTICLE_ID, 85.0, 90.0);

        when(attemptRepository.existsByUserIdAndKuisId(userId, ARTICLE_ID)).thenReturn(false);

        quizService.submitAndSync(userId, request);

        ArgumentCaptor<UserAttempt> captor = ArgumentCaptor.forClass(UserAttempt.class);
        verify(attemptRepository).saveAndFlush(captor.capture());
        verify(quizSyncClient).sync(request);

        UserAttempt savedAttempt = captor.getValue();
        assertEquals(userId, savedAttempt.getUserId());
        assertEquals(ARTICLE_ID, savedAttempt.getKuisId());
        assertNotNull(savedAttempt.getCompletedAt());
    }

    @Test
    void submitAndSync_whenUserIdMissing_throwsBadRequest() {
        QuizSyncRequest request = new QuizSyncRequest(null, ARTICLE_ID, 80.0, 90.0);

        assertThrows(BadRequestException.class, () -> quizService.submitAndSync(null, request));

        verify(attemptRepository, never()).saveAndFlush(any(UserAttempt.class));
        verify(quizSyncClient, never()).sync(any());
    }

    @Test
    void submitAndSync_shouldUseAuthenticatedUserIdInsteadOfRequestBodyUserId() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID spoofedUserId = UUID.randomUUID();
        QuizSyncRequest request = new QuizSyncRequest(spoofedUserId, ARTICLE_ID, 85.0, 90.0);

        when(attemptRepository.existsByUserIdAndKuisId(authenticatedUserId, ARTICLE_ID)).thenReturn(false);

        quizService.submitAndSync(authenticatedUserId, request);

        ArgumentCaptor<UserAttempt> captor = ArgumentCaptor.forClass(UserAttempt.class);
        verify(attemptRepository).saveAndFlush(captor.capture());
        assertEquals(authenticatedUserId, captor.getValue().getUserId());
        assertEquals(authenticatedUserId, request.getUserId());
        verify(quizSyncClient).sync(request);
    }
}
