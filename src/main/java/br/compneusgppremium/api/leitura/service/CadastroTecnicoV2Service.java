package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.controller.dto.CarcacaEntradaDTO;
import br.compneusgppremium.api.controller.dto.CarcacaRespostaDTO;
import br.compneusgppremium.api.controller.dto.ReferenciaPneuDTO;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.dto.CadastroTecnicoV2DTO;
import br.compneusgppremium.api.leitura.repository.ImagemLeituraRepository;
import br.compneusgppremium.api.leitura.repository.SessaoLeituraRepository;
import br.compneusgppremium.api.service.CarcacaCadastroService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CadastroTecnicoV2Service {
    private final SessaoLeituraService sessoes;
    private final SessaoLeituraRepository repositorio;
    private final ImagemLeituraRepository imagens;
    private final CarcacaCadastroService cadastro;
    private final LeituraJson json;
    @Transactional
    public CarcacaRespostaDTO salvar(final String chave, final CadastroTecnicoV2DTO entrada, final Integer carcacaId) {
        String id = entrada.sessaoId().toString();
        var sessao = sessoes.buscar(id);
        String hash = ArquivosLeituraService.hash((json.escrever(entrada) + "|" + carcacaId).getBytes(StandardCharsets.UTF_8));
        if ("VINCULADA".equals(sessao.getStatus())) {
            if (!chave.equals(sessao.getChaveCadastro()) || !hash.equals(sessao.getHashCadastro())) {
                throw sessoes.conflito("Sessão vinculada por outra requisição de cadastro.");
            }
            return cadastro.consultar(sessao.getCarcacaId());
        }
        sessao = sessoes.bloquear(id, entrada.versaoSessao());
        if (!Objects.equals(carcacaId, sessao.getCarcacaId())) {
            throw sessoes.conflito("Sessão não pertence à carcaça indicada.");
        }
        if (entrada.confirmarCombinacaoNova()
                && (entrada.motivoCombinacaoNova() == null || entrada.motivoCombinacaoNova().isBlank())) {
            throw new CadastroPneuException("MOTIVO_OBRIGATORIO", "Informe o motivo da confirmação da combinação nova.");
        }
        var campos = sessoes.confirmacoes(sessao);
        if (!campos.keySet().containsAll(List.of("MARCA", "MODELO", "MEDIDA", "PAIS", "DOT"))) {
            throw new CadastroPneuException("CAMPOS_NAO_CONFIRMADOS", "Confirme todos os campos antes de salvar.");
        }
        CarcacaEntradaDTO dados = new CarcacaEntradaDTO(); dados.setNumero_etiqueta(entrada.numeroEtiqueta());
        dados.setMarca(new ReferenciaPneuDTO(campos.get("MARCA").itemFinalId()));
        dados.setModelo(new ReferenciaPneuDTO(campos.get("MODELO").itemFinalId()));
        dados.setMedida(new ReferenciaPneuDTO(campos.get("MEDIDA").itemFinalId()));
        dados.setPais(new ReferenciaPneuDTO(campos.get("PAIS").itemFinalId())); dados.setDot(campos.get("DOT").valorFinal());
        dados.setFotos(json.escrever(imagens.findBySessaoId(id).stream()
                .map(imagem -> "/api/v2/leitura-imagens/" + imagem.getId()).toList()));
        CarcacaRespostaDTO resposta;
        if (carcacaId == null) {
            resposta = cadastro.criar(dados, entrada.confirmarCombinacaoNova());
        } else {
            resposta = cadastro.atualizar(carcacaId, dados, entrada.confirmarCombinacaoNova());
        }
        sessao.setCarcacaId(resposta.id()); sessao.setGrupoFisico(resposta.uuid() == null
                ? java.util.UUID.nameUUIDFromBytes(("carcaca:" + resposta.id()).getBytes(StandardCharsets.UTF_8)).toString()
                : resposta.uuid().toString());
        sessao.setMotivoCombinacaoNova(entrada.motivoCombinacaoNova());
        sessao.setChaveCadastro(chave); sessao.setHashCadastro(hash); sessao.setStatus("VINCULADA");
        repositorio.saveAndFlush(sessao); return resposta;
    }
}
