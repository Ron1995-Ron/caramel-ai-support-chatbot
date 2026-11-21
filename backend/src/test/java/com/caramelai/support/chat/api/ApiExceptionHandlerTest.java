package com.caramelai.support.chat.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

  record Body(String content) {}

  @RestController
  static class Probe {
    @PostMapping("/probe")
    String post(@RequestBody Body body) {
      return "ok";
    }
  }

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new Probe())
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void wrongHttpMethodIs405NotA500() throws Exception {
    mvc.perform(get("/probe"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"));
  }

  @Test
  void malformedJsonIs400NotA500() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void unsupportedMediaTypeIs415() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.TEXT_PLAIN).content("hi"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
