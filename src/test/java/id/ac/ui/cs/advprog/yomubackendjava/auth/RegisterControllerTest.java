package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterControllerTest {
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String PASSWORD_PLAINTEXT = "rahasia123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerWithEmailShouldReturnWrappedSuccess() throws Exception {
        String requestJson = """
                {
                  "username": "pelajar_email",
                  "display_name": "Pelajar Email",
                  "password": "rahasia123",
                  "email": "pelajar.email@example.com"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("PELAJAR"))
                .andExpect(jsonPath("$.data.user.display_name").value("Pelajar Email"));
    }

    @Test
    void registerWithPhoneNumberShouldReturnWrappedSuccess() throws Exception {
        String requestJson = """
                {
                  "username": "pelajar_phone",
                  "display_name": "Pelajar Phone",
                  "password": "rahasia123",
                  "phone_number": "+62811222333444"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phone_number").value("+62811222333444"));
    }

    @Test
    void registerWithoutEmailAndPhoneShouldReturn400() throws Exception {
        String requestJson = """
                {
                  "username": "pelajar_invalid",
                  "display_name": "Pelajar Invalid",
                  "password": "rahasia123"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void registerWithDuplicateUsernameShouldReturn409() throws Exception {
        String firstRequestJson = """
                {
                  "username": "username_sama",
                  "display_name": "Pertama",
                  "password": "rahasia123",
                  "email": "pertama@example.com"
                }
                """;
        String secondRequestJson = """
                {
                  "username": "username_sama",
                  "display_name": "Kedua",
                  "password": "rahasia456",
                  "email": "kedua@example.com"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(firstRequestJson))
                .andExpect(status().isOk());

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(secondRequestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void registerWithDuplicateEmailShouldReturn409() throws Exception {
        String firstRequestJson = """
                {
                  "username": "email_first",
                  "display_name": "Email Pertama",
                  "password": "rahasia123",
                  "email": "duplikat@example.com"
                }
                """;
        String secondRequestJson = """
                {
                  "username": "email_second",
                  "display_name": "Email Kedua",
                  "password": "rahasia456",
                  "email": "duplikat@example.com"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(firstRequestJson))
                .andExpect(status().isOk());

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(secondRequestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void registerWithDuplicatePhoneShouldReturn409() throws Exception {
        String firstRequestJson = """
                {
                  "username": "phone_first",
                  "display_name": "Phone Pertama",
                  "password": "rahasia123",
                  "phone_number": "+62819000111222"
                }
                """;
        String secondRequestJson = """
                {
                  "username": "phone_second",
                  "display_name": "Phone Kedua",
                  "password": "rahasia456",
                  "phone_number": "+62819000111222"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(firstRequestJson))
                .andExpect(status().isOk());

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(secondRequestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void savedPasswordHashShouldNotMatchPlainPassword() throws Exception {
        String requestJson = """
                {
                  "username": "hash_check_user",
                  "display_name": "Hash Check User",
                  "password": "rahasia123",
                  "email": "hashcheck@example.com"
                }
                """;

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        Optional<UserEntity> maybeUser = userRepository.findByUsernameAndDeletedAtIsNull("hash_check_user");
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.get().getPasswordHash()).isNotEqualTo(PASSWORD_PLAINTEXT);
    }
}
