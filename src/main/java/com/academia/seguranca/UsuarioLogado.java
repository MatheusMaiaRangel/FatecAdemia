package com.academia.seguranca;

import com.academia.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioLogado {

    static final String SEM_USUARIO_LOGADO = "Nenhum usuário autenticado nesta requisição";

    // O usuário vem do filtro JWT, fora de transação: serve para comparar ids e associar a outras
    // entidades, mas coleções lazy dele não podem ser navegadas a partir daqui.
    public Usuario obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Usuario usuario)) {
            throw new IllegalStateException(SEM_USUARIO_LOGADO);
        }
        return usuario;
    }

    public Long obterId() {
        return obter().getId();
    }
}
