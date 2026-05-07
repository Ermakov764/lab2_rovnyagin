package ru.hse.lab8.additional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Лаб. 14: оба микросервиса должны отдавать Swagger UI.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void v3ApiDocsReturnsOpenApiJson() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void swaggerUiHtmlRedirectsOrOk() throws Exception {
        var result = mockMvc.perform(get("/swagger-ui.html")).andReturn();
        int sc = result.getResponse().getStatus();
        assertTrue(sc == 200 || (sc >= 300 && sc < 400),
                "Swagger UI expected 200 or redirect, got " + sc);
    }

    /** В браузере Springdoc 3 часто открывают именно этот URL (как у основного сервиса). */
    @Test
    void swaggerUiIndexIsReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
