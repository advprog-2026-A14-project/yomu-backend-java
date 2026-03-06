package id.ac.ui.cs.advprog.yomubackendjava.auth.google;

public interface GoogleIdTokenVerifier {
    GoogleProfile verify(String idToken);
}
