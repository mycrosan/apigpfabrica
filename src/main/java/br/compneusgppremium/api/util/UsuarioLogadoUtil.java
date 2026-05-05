package br.compneusgppremium.api.util;

import br.compneusgppremium.api.controller.dto.CustomUserDetails;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioLogadoUtil {

    public Long getUsuarioIdLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();

            if (principal instanceof UsuarioModel) {
                return ((UsuarioModel) principal).getId();
            }

            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getId();
            }

            if (principal instanceof String) {
                try {
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Principal não é um ID válido");
                }
            }
        }

        throw new RuntimeException("Usuário não autenticado");
    }
}
