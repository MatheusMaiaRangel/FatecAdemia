package com.academia.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioService usuarioService;
    private final AdminProperties admin;

    public AdminInicial(UsuarioService usuarioService, AdminProperties admin) {
        this.usuarioService = usuarioService;
        this.admin = admin;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (usuarioService.criarAdminSeNaoExistir(admin)) {
            log.info("Administrador inicial criado com o e-mail {}", admin.email());
        } else {
            log.info("Administrador inicial já existe, nada a fazer");
        }
    }
}
