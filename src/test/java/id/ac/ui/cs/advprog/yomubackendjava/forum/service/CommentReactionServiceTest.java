package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.CommentReaction;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentReactionRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.strategy.ReactionStrategy;
import id.ac.ui.cs.advprog.yomubackendjava.forum.strategy.ReactionStrategyFactory;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentReactionServiceTest {

    @Mock
    private CommentReactionRepository reactionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReactionStrategyFactory strategyFactory;

    @Mock
    private ReactionStrategy reactionStrategy;

    @InjectMocks
    private CommentReactionService reactionService;

    private UUID commentId;
    private UUID userId;
    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleComment = new Comment();
        sampleComment.setId(commentId);
        sampleComment.setArticleId(UUID.randomUUID());
        sampleComment.setUserId(userId);
        sampleComment.setContent("Komentar test");
        sampleComment.setCreatedAt(Instant.now());
        sampleComment.setReplies(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID id, Role role) {
        JwtService.JwtClaims claims = new JwtService.JwtClaims(
                id, role, Instant.now(), Instant.now().plusSeconds(3600));
        String authority = "ROLE_" + role.name();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void toggleReaction_notYetReacted_shouldAddReactionAndCallStrategy() {
        authenticateAs(userId, Role.PELAJAR);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(strategyFactory.resolve(ReactionType.UPVOTE)).thenReturn(reactionStrategy);
        when(reactionRepository.findByCommentIdAndUserIdAndReactionType(commentId, userId, ReactionType.UPVOTE))
                .thenReturn(Optional.empty());
        when(reactionRepository.countByCommentIdAndReactionType(commentId, ReactionType.UPVOTE))
                .thenReturn(1);

        ReactionResponse response = reactionService.toggleReaction(commentId, ReactionType.UPVOTE);

        assertTrue(response.isReacted());
        assertEquals(1, response.getReactionCount());
        assertEquals(ReactionType.UPVOTE, response.getReactionType());
        verify(reactionRepository).save(any(CommentReaction.class));
        verify(reactionStrategy).onReactionAdded(commentId, userId);
    }

    @Test
    void toggleReaction_alreadyReacted_shouldRemoveReactionAndCallStrategy() {
        authenticateAs(userId, Role.PELAJAR);

        CommentReaction existing = new CommentReaction(UUID.randomUUID(), commentId, userId, ReactionType.UPVOTE);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(strategyFactory.resolve(ReactionType.UPVOTE)).thenReturn(reactionStrategy);
        when(reactionRepository.findByCommentIdAndUserIdAndReactionType(commentId, userId, ReactionType.UPVOTE))
                .thenReturn(Optional.of(existing));
        when(reactionRepository.countByCommentIdAndReactionType(commentId, ReactionType.UPVOTE))
                .thenReturn(0);

        ReactionResponse response = reactionService.toggleReaction(commentId, ReactionType.UPVOTE);

        assertFalse(response.isReacted());
        assertEquals(0, response.getReactionCount());
        verify(reactionRepository).delete(existing);
        verify(reactionStrategy).onReactionRemoved(commentId, userId);
    }

    @Test
    void toggleReaction_noAuth_shouldThrowUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> reactionService.toggleReaction(commentId, ReactionType.UPVOTE));
        verifyNoInteractions(reactionRepository);
    }

    @Test
    void toggleReaction_commentNotFound_shouldThrowNotFound() {
        authenticateAs(userId, Role.PELAJAR);

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> reactionService.toggleReaction(commentId, ReactionType.UPVOTE));
        verifyNoInteractions(reactionRepository);
    }

    @Test
    void getReactionCount_shouldReturnCorrectCount() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(reactionRepository.countByCommentIdAndReactionType(commentId, ReactionType.UPVOTE))
                .thenReturn(5);

        int count = reactionService.getReactionCount(commentId, ReactionType.UPVOTE);

        assertEquals(5, count);
    }

    @Test
    void getReactionCount_commentNotFound_shouldThrowNotFound() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> reactionService.getReactionCount(commentId, ReactionType.UPVOTE));
    }

    @Test
    void toggleReaction_unsupportedReactionType_shouldThrowIllegalArgument() {
        authenticateAs(userId, Role.PELAJAR);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(strategyFactory.resolve(any())).thenThrow(new IllegalArgumentException("Reaction type tidak didukung"));

        assertThrows(IllegalArgumentException.class,
                () -> reactionService.toggleReaction(commentId, ReactionType.UPVOTE));
    }
}
