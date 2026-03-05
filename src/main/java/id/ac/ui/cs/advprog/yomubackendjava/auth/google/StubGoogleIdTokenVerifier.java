package id.ac.ui.cs.advprog.yomubackendjava.auth.google;

import org.springframework.stereotype.Component;

@Component
public class StubGoogleIdTokenVerifier implements GoogleIdTokenVerifier {
    @Override
    public GoogleProfile verify(String idToken) {
        throw new IllegalArgumentException("Google ID token verifier belum dikonfigurasi");
    }
}
