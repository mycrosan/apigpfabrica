package br.compneusgppremium.api.service;
import br.compneusgppremium.api.controller.dto.CarcacaEntradaDTO;
import br.compneusgppremium.api.controller.dto.CarcacaRespostaDTO;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.mapper.CarcacaMapper;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.StatusCarcacaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarcacaCadastroService {
    private final CarcacaRepository carcaças;
    private final StatusCarcacaRepository status;
    private final UsuarioRepository usuarios;
    private final UsuarioLogadoUtil usuarioLogado;
    private final ValidacaoCadastroPneuService validacao;
    private final CarcacaMapper mapper;
    private final ObjectMapper json;
    private final Clock relogio;
    private final br.compneusgppremium.api.revisao.service.ImpactoCadastroRevisaoService revisoes;
    public List<CarcacaRespostaDTO> listar() {
        return carcaças.listarRecentes(org.springframework.data.domain.PageRequest.of(0, 50))
                .stream().map(mapper::resposta).toList();
    }
    public CarcacaRespostaDTO consultar(final Integer id) {
        return mapper.resposta(buscar(id));
    }
    public CarcacaRespostaDTO pesquisar(final String etiqueta) {
        List<CarcacaModel> encontrados = carcaças.buscarEtiqueta(etiqueta);
        if (encontrados.isEmpty()) {
            throw ausente();
        }
        if (encontrados.size() != 1) {
            throw new CadastroPneuException("ETIQUETA_DUPLICADA", "Etiqueta duplicada; encaminhe à revisão.");
        }
        return mapper.resposta(encontrados.get(0));
    }
    @Transactional
    public CarcacaRespostaDTO criar(final CarcacaEntradaDTO entrada, final boolean confirmarNova) {
        CarcacaModel entidade = new CarcacaModel();
        preencher(entrada, confirmarNova, entidade);
        entidade.setStatus("start");
        entidade.setStatus_carcaca(status.findById(1).orElseThrow(() ->
                new CadastroPneuException("STATUS_AUSENTE", "Status inicial não configurado.")));
        entidade.setCriadoPor(usuarios.findById(usuarioLogado.getUsuarioIdLogado()).orElseThrow(this::ausente));
        entidade.setDt_create(Date.from(relogio.instant()));
        entidade.setUuid(UUID.randomUUID());
        entidade.setDados("{}");
        entidade.setFotos(entrada.getFotos());
        entidade.setLeituraMetadados(auditar(entrada.getLeituraMetadados()));
        CarcacaModel salva = carcaças.save(entidade);
        log.info("Carcaça cadastrada id={}", salva.getId());
        return mapper.resposta(salva);
    }
    @Transactional
    public CarcacaRespostaDTO atualizar(final Integer id, final CarcacaEntradaDTO entrada, final boolean confirmarNova) {
        CarcacaModel entidade = buscar(id);
        preencher(entrada, confirmarNova, entidade);
        if (entrada.getFotos() != null && !entrada.getFotos().isBlank()) {
            entidade.setFotos(unirFotos(entidade.getFotos(), entrada.getFotos()));
        }
        log.info("Dados técnicos atualizados carcacaId={}", id);
        revisoes.cadastroAlterado(id);
        return mapper.resposta(carcaças.save(entidade));
    }
    @Transactional
    public void excluir(final Integer id) {
        carcaças.delete(buscar(id));
    }
    private void preencher(final CarcacaEntradaDTO entrada, final boolean confirmarNova, final CarcacaModel entidade) {
        ValidacaoCadastroPneuService.Referencias referencias = validacao.validar(entrada, confirmarNova);
        boolean duplicada = carcaças.buscarEtiqueta(entrada.getNumero_etiqueta()).stream()
                .anyMatch(outra -> !outra.getId().equals(entidade.getId()));
        if (duplicada) {
            throw new CadastroPneuException("ETIQUETA_DUPLICADA", "Etiqueta já cadastrada.", HttpStatus.CONFLICT);
        }
        mapper.atualizar(entrada, entidade);
        entidade.setModelo(referencias.modelo());
        entidade.setMedida(referencias.medida());
        entidade.setPais(referencias.pais());
        entidade.setDt_update(Date.from(relogio.instant()));
    }
    private String unirFotos(final String existentes, final String recebidas) {
        try {
            var imagens = new java.util.LinkedHashSet<String>();
            if (existentes != null && !existentes.isBlank()) {
                imagens.addAll(json.readValue(existentes, new TypeReference<List<String>>() {}));
            }
            imagens.addAll(json.readValue(recebidas, new TypeReference<List<String>>() {}));
            return json.writeValueAsString(imagens);
        } catch (JsonProcessingException erro) {
            throw new CadastroPneuException("FOTOS_INVALIDAS", "Referências de imagens inválidas.");
        }
    }
    private String auditar(final String metadados) {
        if (metadados == null || metadados.isBlank()) {
            return null;
        }
        try {
            List<Map<String, Object>> eventos = json.readValue(metadados, new TypeReference<>() {});
            for (Map<String, Object> evento : eventos) {
                evento.put("usuarioId", usuarioLogado.getUsuarioIdLogado());
                evento.put("dataHora", relogio.instant().toString());
                evento.put("revisaoTreinamento", "PENDENTE");
            }
            return json.writeValueAsString(eventos);
        } catch (JsonProcessingException | IllegalArgumentException erro) {
            throw new CadastroPneuException("METADADOS_INVALIDOS", "Metadados de leitura inválidos.");
        }
    }
    private CarcacaModel buscar(final Integer id) {
        return carcaças.findById(id).orElseThrow(this::ausente);
    }
    private CadastroPneuException ausente() {
        return new CadastroPneuException("NAO_ENCONTRADO", "Registro não encontrado.", HttpStatus.NOT_FOUND);
    }
}
