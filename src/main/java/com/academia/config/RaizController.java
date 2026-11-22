package com.academia.config;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class RaizController {

    static final String ROTA_DO_SWAGGER = "/swagger-ui.html";

    @GetMapping("/")
    public String redirecionarParaDocumentacao() {
        return "redirect:" + ROTA_DO_SWAGGER;
    }
}
