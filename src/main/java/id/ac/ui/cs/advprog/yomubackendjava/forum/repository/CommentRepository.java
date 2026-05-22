package id.ac.ui.cs.advprog.yomubackendjava.forum.repository;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(String articleId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.articleId = :articleId")
    void deleteByArticleId(@Param("articleId") String articleId);
}
