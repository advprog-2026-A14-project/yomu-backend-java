package id.ac.ui.cs.advprog.yomubackendjava.forum.exception;

import java.util.UUID;

public class CommentNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CommentNotFoundException(UUID commentId) {
        super("Komentar dengan ID " + commentId + " tidak ditemukan");
    }
}
