package com.hyperlogix.server.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OpenApiController {

  @GetMapping("/openapi")
  public String redirectToSwaggerUi() {
    return "redirect:/swagger-ui.html";
  }
}
