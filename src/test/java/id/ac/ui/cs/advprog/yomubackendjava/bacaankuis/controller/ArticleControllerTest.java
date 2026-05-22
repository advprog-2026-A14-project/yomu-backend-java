package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testListArticlesReturnsOk() throws Exception {
        UserEntity user = saveUser();

        mockMvc.perform(get("/api/v1/articles")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX
                                + jwtService.generateToken(user.getUserId(), user.getRole())))
                .andExpect(status().isOk());
    }

    private UserEntity saveUser() {
        UserEntity user = new UserEntity();
        user.setUsername("article_reader");
        user.setDisplayName("Article Reader");
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        return userRepository.saveAndFlush(user);
    }
}
