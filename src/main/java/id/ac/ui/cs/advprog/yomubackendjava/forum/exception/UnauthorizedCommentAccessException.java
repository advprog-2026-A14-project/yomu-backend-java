package id.ac.ui.cs.advprog.yomubackendjava.forum.exception;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;

public class UnauthorizedCommentAccessException extends ForbiddenException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedCommentAccessException() {
        super("Anda tidak memiliki izin untuk mengubah komentar ini");
    }
}