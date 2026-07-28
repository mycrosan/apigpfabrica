package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.dto.ClassificacaoCombinacaoDTO;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.RegraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Classifica a combinação modelo+medida+país de uma carcaça antes de salvar,
 * pra impedir que o cadastro registre um pneu que a fábrica não sabe produzir.
 *
 * VERDE    = existe regra de produção pra esse trio (caminho normal, ~90% dos casos hoje).
 * AMARELO  = já foi cadastrado antes mas ainda não tem regra (provável regra faltando).
 * VERMELHO = trio nunca visto — bloqueia o cadastro até confirmação explícita.
 *
 * Também serve a cascata do formulário: sugere as medidas plausíveis pra um
 * modelo, a partir da união de `regra` (fonte oficial) com o histórico real de
 * `carcaca` (fonte observacional, com um mínimo de ocorrências pra não confiar
 * em erro antigo isolado).
 */
@Service
public class CombinacaoPneuService {

    // Uma ocorrência isolada no histórico pode ser um erro antigo; a partir de
    // 2 ocorrências já dá pra confiar que a combinação foi conferida antes.
    private static final long MIN_OCORRENCIAS_MEDIDA_PLAUSIVEL = 2;

    @Autowired
    private RegraRepository regraRepository;
    @Autowired
    private CarcacaRepository carcacaRepository;
    @Autowired
    private MedidaRepository medidaRepository;

    public ClassificacaoCombinacaoDTO classificar(Integer modeloId, Integer medidaId, Integer paisId) {
        ClassificacaoCombinacaoDTO dto = new ClassificacaoCombinacaoDTO();

        if (modeloId == null || medidaId == null || paisId == null) {
            dto.setClassificacao("VERMELHO");
            dto.setMensagem("Modelo, medida e país são obrigatórios para validar a combinação.");
            return dto;
        }

        if (regraRepository.existeParaTrio(modeloId, medidaId, paisId)) {
            dto.setClassificacao("VERDE");
            dto.setMensagem("Combinação com regra de produção cadastrada.");
            return dto;
        }

        if (carcacaRepository.countPorTrio(modeloId, medidaId, paisId) > 0) {
            dto.setClassificacao("AMARELO");
            dto.setMensagem("Essa combinação já foi cadastrada antes, mas ainda não tem regra de "
                    + "produção cadastrada — provavelmente falta cadastrar a regra.");
            return dto;
        }

        dto.setClassificacao("VERMELHO");
        dto.setMensagem("Essa combinação de modelo, medida e país nunca foi cadastrada nem tem regra "
                + "de produção. Confira os dados antes de continuar.");
        return dto;
    }

    public List<MedidaModel> medidasPlausiveis(Integer modeloId) {
        if (modeloId == null) {
            return new ArrayList<>();
        }

        Set<Integer> ids = new LinkedHashSet<>();
        ids.addAll(regraRepository.findMedidaIdsPorModelo(modeloId));
        ids.addAll(carcacaRepository.findMedidaIdsFrequentesPorModelo(modeloId, MIN_OCORRENCIAS_MEDIDA_PLAUSIVEL));

        List<MedidaModel> medidas = new ArrayList<>();
        for (Integer id : ids) {
            medidaRepository.findById(id).ifPresent(medidas::add);
        }
        medidas.sort((a, b) -> a.descricao.compareTo(b.descricao));
        return medidas;
    }
}
