package br.compneusgppremium.api.service;
import br.compneusgppremium.api.controller.dto.CarcacaEntradaDTO;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.ModeloModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.repository.CarcacaRejeitadaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidacaoCadastroPneuService {
    private final Validator validator;
    private final ValidacaoFormatoPneuService formatos;
    private final ModeloRepository modelos;
    private final MedidaRepository medidas;
    private final PaisRepository paises;
    private final CarcacaRejeitadaRepository rejeitadas;
    private final CombinacaoPneuService combinacoes;
    public record Referencias(ModeloModel modelo, MedidaModel medida, PaisModel pais) {}
    public Referencias validar(final CarcacaEntradaDTO entrada, final boolean confirmarNova) {
        if (entrada == null || !validator.validate(entrada).isEmpty()) {
            throw new CadastroPneuException("DADOS_INVALIDOS", "Informe etiqueta, modelo, medida, país e DOT válidos.");
        }
        formatos.validarDot(entrada.getDot());
        ModeloModel modelo = modelos.findById(entrada.getModelo().id()).orElseThrow(() -> invalida("Modelo"));
        if (modelo.getMarca() == null || modelo.getMarca().getId() == null) {
            throw invalida("Marca do modelo");
        }
        if (entrada.getMarca() != null && !entrada.getMarca().id().equals(modelo.getMarca().getId())) {
            throw new CadastroPneuException("MARCA_MODELO_INCOMPATIVEL", "O modelo não pertence à marca informada.");
        }
        MedidaModel medida = medidas.findById(entrada.getMedida().id()).orElseThrow(() -> invalida("Medida"));
        PaisModel pais = paises.findById(entrada.getPais().id()).orElseThrow(() -> invalida("País"));
        formatos.validarMedida(medida.getDescricao());
        if (rejeitadas.existeTrio(modelo.getId(), medida.getId(), pais.getId())) {
            throw new CadastroPneuException("CARCACA_PROIBIDA", "Carcaça proibida para esta combinação.");
        }
        if ("VERMELHO".equals(combinacoes.classificar(modelo.getId(), medida.getId(), pais.getId()).getClassificacao())
                && !confirmarNova) {
            throw new CadastroPneuException("COMBINACAO_NAO_RECONHECIDA", "Confira e confirme a combinação inédita.");
        }
        return new Referencias(modelo, medida, pais);
    }
    private CadastroPneuException invalida(final String campo) {
        return new CadastroPneuException("REFERENCIA_INVALIDA", campo + " não encontrado no catálogo.");
    }
}
