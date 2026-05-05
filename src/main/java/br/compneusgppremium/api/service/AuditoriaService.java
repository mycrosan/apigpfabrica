package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.model.AuditoriaModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.AuditoriaRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioLogadoUtil usuarioLogadoUtil;

    @Transactional
    public void registrar(String tabelaAfetada, Long registroId, String acao, UsuarioModel usuario) {
        AuditoriaModel audit = new AuditoriaModel();
        audit.setTabelaAfetada(tabelaAfetada);
        audit.setRegistroId(registroId);
        audit.setAcao(acao);
        audit.setUsuario(usuario);
        audit.setDataAcao(new Date());
        auditoriaRepository.save(audit);
    }
}

