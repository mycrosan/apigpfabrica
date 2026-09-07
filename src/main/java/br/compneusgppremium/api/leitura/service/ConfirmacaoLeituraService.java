package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.dto.*;
import br.compneusgppremium.api.leitura.model.ConfirmacaoLeitura;
import br.compneusgppremium.api.leitura.repository.*;
import br.compneusgppremium.api.repository.*;
import br.compneusgppremium.api.service.ValidacaoFormatoPneuService;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ConfirmacaoLeituraService {
    private final SessaoLeituraService sessoes;
    private final SessaoLeituraRepository repositorio;
    private final TentativaLeituraRepository tentativas;
    private final ConfirmacaoLeituraRepository confirmacoes;
    private final MarcaRepository marcas;
    private final ModeloRepository modelos;
    private final MedidaRepository medidas;
    private final PaisRepository paises;
    private final ValidacaoFormatoPneuService formatos;
    private final LeituraJson json;
    private final Clock relogio;
    private final br.compneusgppremium.api.revisao.service.ImpactoCadastroRevisaoService revisoes;
    @Transactional
    public SessaoLeituraDTO confirmar(final String id, final ConfirmarCampoDTO entrada) {
        var sessao = sessoes.bloquear(id, entrada.versaoSessao());
        Map<String, CampoConfirmadoDTO> campos = sessoes.confirmacoes(sessao);
        String valor = resolverValor(entrada, campos);
        if (entrada.origem() != ConfirmarCampoDTO.Origem.IA_CONFIRMADA
                && (entrada.motivo() == null || entrada.motivo().isBlank())) {
            throw invalida("Informe o motivo do preenchimento manual ou da correção.");
        }
        if (entrada.origem() != ConfirmarCampoDTO.Origem.MANUAL && entrada.tentativaId() == null) {
            throw invalida("Confirmação ou correção deve indicar a tentativa original.");
        }
        if (entrada.tentativaId() != null) {
            var tentativa = tentativas.findById(entrada.tentativaId()).orElseThrow(sessoes::ausente);
            if (!id.equals(tentativa.getSessaoId()) || !entrada.campo().name().equals(tentativa.getCampo())
                    || !entrada.versaoSessao().equals(tentativa.getVersaoSessao())) {
                throw sessoes.conflito("Tentativa não corresponde ao campo e contexto atuais.");
            }
            if (entrada.origem() == ConfirmarCampoDTO.Origem.IA_CONFIRMADA) {
                if (tentativa.getRespostaJson() == null) {
                    throw invalida("Tentativa ainda sem resultado.");
                }
                var resultado = json.ler(tentativa.getRespostaJson(), ResultadoLeituraDTO.class);
                if (!"SUGESTAO".equals(resultado.estado()) || !Objects.equals(entrada.itemFinalId(), resultado.itemSugeridoId())
                        || !valor.equals(resultado.valorSugerido())) {
                    throw invalida("Valor diferente da sugestão; registre como correção.");
                }
            }
        }
        String confirmacaoId = UUID.randomUUID().toString();
        var anterior = campos.get(entrada.campo().name());
        if (anterior == null || !Objects.equals(anterior.itemFinalId(), entrada.itemFinalId())
                || !Objects.equals(anterior.valorFinal(), valor)) {
            switch (entrada.campo()) {
                case MARCA -> { campos.remove("MODELO"); campos.remove("MEDIDA"); campos.remove("PAIS"); }
                case MODELO -> { campos.remove("MEDIDA"); campos.remove("PAIS"); }
                case MEDIDA -> campos.remove("PAIS");
                default -> { }
            }
        }
        campos.put(entrada.campo().name(), new CampoConfirmadoDTO(entrada.itemFinalId(), valor,
                entrada.origem().name(), entrada.tentativaId(), confirmacaoId));
        sessao.setConfirmacoesJson(json.escrever(campos)); repositorio.saveAndFlush(sessao);
        ConfirmacaoLeitura evento = new ConfirmacaoLeitura(); evento.setId(confirmacaoId); evento.setSessaoId(id);
        evento.setCampo(entrada.campo().name()); evento.setOrigem(entrada.origem().name());
        evento.setTentativaId(entrada.tentativaId()); evento.setItemFinalId(entrada.itemFinalId());
        evento.setValorFinal(valor); evento.setMotivo(entrada.motivo()); evento.setOperadorId(sessao.getOperadorId());
        evento.setCriadaEm(relogio.instant()); evento.setVersaoSessao(sessao.getVersao()); confirmacoes.saveAndFlush(evento);
        revisoes.confirmacao(evento);
        return sessoes.resposta(sessao);
    }
    private String resolverValor(final ConfirmarCampoDTO entrada, final Map<String, CampoConfirmadoDTO> campos) {
        if (entrada.campo() == br.compneusgppremium.api.service.LeituraCarcacaService.Campo.DOT) {
            if (entrada.itemFinalId() != null) {
                throw invalida("DOT não possui ID de catálogo.");
            }
            formatos.validarDot(entrada.valorFinal()); return entrada.valorFinal();
        }
        if (entrada.itemFinalId() == null) {
            throw invalida("Selecione um item do catálogo.");
        }
        return switch (entrada.campo()) {
            case MARCA -> marcas.findById(entrada.itemFinalId()).orElseThrow(sessoes::ausente).getDescricao();
            case MODELO -> {
                var modelo = modelos.findById(entrada.itemFinalId()).orElseThrow(sessoes::ausente);
                var marca = campos.get("MARCA");
                if (marca == null || modelo.getMarca() == null || !marca.itemFinalId().equals(modelo.getMarca().getId())) {
                    throw invalida("Confirme a marca correta antes do modelo.");
                }
                yield modelo.getDescricao();
            }
            case MEDIDA -> {
                if (!campos.containsKey("MODELO")) { throw invalida("Confirme o modelo antes da medida."); }
                var medida = medidas.findById(entrada.itemFinalId()).orElseThrow(sessoes::ausente);
                formatos.validarMedida(medida.getDescricao()); yield medida.getDescricao();
            }
            case PAIS -> paises.findById(entrada.itemFinalId()).orElseThrow(sessoes::ausente).getDescricao();
            default -> throw invalida("Campo inválido.");
        };
    }
    private CadastroPneuException invalida(final String mensagem) {
        return new CadastroPneuException("CONFIRMACAO_INVALIDA", mensagem);
    }
}
