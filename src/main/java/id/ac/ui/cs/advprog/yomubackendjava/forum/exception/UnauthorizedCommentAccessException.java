package id.ac.ui.cs.advprog.yomubackendjava.forum.exception;

public class UnauthorizedCommentAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedCommentAccessException() {
        super("Anda tidak memiliki izin untuk mengubah komentar ini");
    }
}
