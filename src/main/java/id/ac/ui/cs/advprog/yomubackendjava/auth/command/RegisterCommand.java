package id.ac.ui.cs.advprog.yomubackendjava.auth.command;

public record RegisterCommand(
        String username,
        String displayName,
        String password,
        String email,
        String phoneNumber
) {
}
