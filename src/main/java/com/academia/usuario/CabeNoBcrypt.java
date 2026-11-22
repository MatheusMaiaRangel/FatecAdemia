package com.academia.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

// O limite do BCrypt é em bytes, não em caracteres: letras acentuadas ocupam 2 bytes em UTF-8,
// então @Size sozinho deixaria passar senhas que o BCrypt cortaria ou recusaria.
@Documented
@Constraint(validatedBy = CabeNoBcrypt.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CabeNoBcrypt {

    String message() default "deve ter no máximo " + LimitesUsuario.TAMANHO_MAXIMO_SENHA_EM_BYTES
            + " bytes (letras acentuadas contam como 2)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CabeNoBcrypt, String> {

        @Override
        public boolean isValid(String senha, ConstraintValidatorContext contexto) {
            return senha == null
                    || senha.getBytes(StandardCharsets.UTF_8).length <= LimitesUsuario.TAMANHO_MAXIMO_SENHA_EM_BYTES;
        }
    }
}
