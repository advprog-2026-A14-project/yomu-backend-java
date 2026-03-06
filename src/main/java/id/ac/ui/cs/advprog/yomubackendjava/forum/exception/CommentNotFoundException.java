package id.ac.ui.cs.advprog.yomubackendjava.forum.exception;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;

public class CommentNotFoundException extends NotFoundException {

    public CommentNotFoundException(java.util.UUID commentId) {
        super("Komentar dengan ID " + commentId + " tidak ditemukan");
    }
}