package id.ac.ui.cs.advprog.yomubackendjava.health;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.web.GlobalExceptionHandler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import(GlobalExceptionHandler.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHealthShouldReturnWrappedSuccessWithStatusOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    void apiResponseSuccessWithoutDataShouldNotContainDataField() throws Exception {
        MockMvc standaloneMockMvc = MockMvcBuilders.standaloneSetup(new TestOnlyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        standaloneMockMvc.perform(get("/api/v1/test/no-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No data response"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void conflictExceptionShouldReturnWrappedConflictError() throws Exception {
        MockMvc standaloneMockMvc = MockMvcBuilders.standaloneSetup(new TestOnlyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        standaloneMockMvc.perform(get("/api/v1/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", Matchers.not(Matchers.isEmptyOrNullString())));
    }

    @RestController
    @RequestMapping("/api/v1/test")
    public static class TestOnlyController {
        @GetMapping("/no-data")
        public ApiResponse<Void> noData() {
            return ApiResponse.success("No data response");
        }

        @GetMapping("/conflict")
        public ApiResponse<Void> conflict() {
            throw new ConflictException("Duplicate request");
        }
    }
}
