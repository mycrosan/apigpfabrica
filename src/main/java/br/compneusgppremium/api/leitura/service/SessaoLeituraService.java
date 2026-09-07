package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.dto.*;
import br.compneusgppremium.api.leitura.model.SessaoLeitura;
import br.compneusgppremium.api.leitura.model.TentativaLeitura;
import br.compneusgppremium.api.leitura.repository.*;
import br.compneusgppremium.api.mapper.LeituraMapper;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessaoLeituraService {
    private final SessaoLeituraRepository sessoes;
    private final TentativaLeituraRepository tentativas;
    private final ImagemLeituraRepository imagens;
    private final CarcacaRepository carcacas;
    private final UsuarioLogadoUtil usuario;
    private final LeituraJson json;
    private final LeituraMapper mapper;
    private final ArquivosLeituraService arquivos;
    private final Clock relogio;
    @Transactional
    public SessaoLeituraDTO abrir(final UUID chave, final AbrirSessaoDTO entrada) {
        var existente = sessoes.findById(chave.toString());
        if (existente.isPresent()) {
            SessaoLeitura sessao = autorizar(existente.get());
            if (!java.util.Objects.equals(sessao.getCarcacaInicialId(), entrada.carcacaId())) {
                throw conflito("Chave de sessão utilizada com outro conteúdo.");
            }
            return resposta(sessao);
        }
        SessaoLeitura sessao = new SessaoLeitura();
        sessao.setId(chave.toString()); sessao.setGrupoFisico(chave.toString());
        if (entrada.carcacaId() != null) {
            var carcaca = carcacas.findById(entrada.carcacaId()).orElseThrow(this::ausente);
            if (carcaca.getUuid() != null) {
                sessao.setGrupoFisico(carcaca.getUuid().toString());
            }
        }
        sessao.setCarcacaInicialId(entrada.carcacaId());
        sessao.setCarcacaId(entrada.carcacaId()); sessao.setOperadorId(usuario.getUsuarioIdLogado());
        sessao.setCriadaEm(relogio.instant()); sessao.setStatus("ABERTA"); sessao.setConfirmacoesJson("{}");
        return resposta(sessoes.saveAndFlush(sessao));
    }
    public SessaoLeituraDTO consultar(final String id) {
        return resposta(buscar(id));
    }
    public byte[] imagem(final String id) {
        var imagem = imagens.findById(id).orElseThrow(this::ausente);
        buscar(imagem.getSessaoId());
        byte[] bytes = arquivos.ler(imagem.getArquivo());
        if (!imagem.getSha256().equals(ArquivosLeituraService.hash(bytes))) {
            throw new CadastroPneuException("EVIDENCIA_CORROMPIDA", "Evidência indisponível para conferência.", HttpStatus.CONFLICT);
        }
        return bytes;
    }
    public SessaoLeitura buscar(final String id) {
        return autorizar(sessoes.findById(id).orElseThrow(this::ausente));
    }
    public SessaoLeitura bloquear(final String id, final Long versao) {
        SessaoLeitura sessao = autorizar(sessoes.bloquear(id).orElseThrow(this::ausente));
        if ("ABANDONADA".equals(sessao.getStatus())) {
            throw conflito("Sessão abandonada; abra uma nova sessão para continuar.");
        }
        if (!"ABERTA".equals(sessao.getStatus()) || !sessao.getVersao().equals(versao)) {
            throw conflito("A sessão foi alterada. Atualize os dados antes de continuar.");
        }
        return sessao;
    }
    /**
     * Encerra a sessão sem cadastro, preservando fotos, tentativas e confirmações já registradas.
     *
     * <p>Abandonar não apaga evidência: a coleta interrompida continua auditável e o motivo do
     * operador fica junto da sessão. Depois disso a sessão não aceita nova tentativa, confirmação
     * ou cadastro; o operador abre outra.</p>
     *
     * @param id sessão a encerrar.
     * @param entrada motivo do operador e versão conhecida pelo cliente.
     * @return a sessão no estado final, com o histórico preservado.
     */
    @Transactional
    public SessaoLeituraDTO abandonar(final String id, final AbandonarSessaoDTO entrada) {
        SessaoLeitura sessao = autorizar(sessoes.bloquear(id).orElseThrow(this::ausente));
        if ("ABANDONADA".equals(sessao.getStatus())) {
            // Reenvio após timeout de rede: o desfecho pretendido já é o vigente, não é conflito.
            return resposta(sessao);
        }
        if ("VINCULADA".equals(sessao.getStatus())) {
            throw conflito("Sessão já vinculada a um cadastro; não pode ser abandonada.");
        }
        if (!sessao.getVersao().equals(entrada.versaoSessao())) {
            throw conflito("A sessão foi alterada. Atualize os dados antes de continuar.");
        }
        sessao.setStatus("ABANDONADA");
        sessao.setMotivoAbandono(entrada.motivo());
        // Identidade e horário do abandono são do servidor, nunca do cliente.
        sessao.setAbandonadaEm(relogio.instant());
        sessao.setAbandonadaPor(usuario.getUsuarioIdLogado());
        return resposta(sessoes.saveAndFlush(sessao));
    }
    public Map<String, CampoConfirmadoDTO> confirmacoes(final SessaoLeitura sessao) {
        return new LinkedHashMap<>(json.ler(sessao.getConfirmacoesJson(), new TypeReference<Map<String, CampoConfirmadoDTO>>() {}));
    }
    public SessaoLeituraDTO resposta(final SessaoLeitura sessao) {
        return mapper.sessao(sessao, confirmacoes(sessao), tentativas.findBySessaoIdOrderByCriadaEmAsc(sessao.getId())
                .stream().map(this::respostaTentativa).toList());
    }
    public TentativaLeituraDTO respostaTentativa(final TentativaLeitura tentativa) {
        ResultadoLeituraDTO resultado = null;
        if (tentativa.getRespostaJson() != null) {
            resultado = json.ler(tentativa.getRespostaJson(), ResultadoLeituraDTO.class);
        }
        return mapper.tentativa(tentativa, resultado);
    }
    private SessaoLeitura autorizar(final SessaoLeitura sessao) {
        if (!sessao.getOperadorId().equals(usuario.getUsuarioIdLogado())) {
            throw new CadastroPneuException("ACESSO_NEGADO", "Acesso à sessão não autorizado.", HttpStatus.FORBIDDEN);
        }
        return sessao;
    }
    public CadastroPneuException ausente() {
        return new CadastroPneuException("LEITURA_AUSENTE", "Registro de leitura não encontrado.", HttpStatus.NOT_FOUND);
    }
    public CadastroPneuException conflito(final String mensagem) {
        return new CadastroPneuException("CONFLITO_LEITURA", mensagem, HttpStatus.CONFLICT);
    }
}
