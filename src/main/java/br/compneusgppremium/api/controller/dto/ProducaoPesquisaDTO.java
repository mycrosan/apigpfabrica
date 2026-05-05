package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.RegraModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "DTO para pesquisa e filtros de produção")
public class ProducaoPesquisaDTO {
    public static ProducaoDTO fromModel(ProducaoModel p) {
        return new ProducaoDTO(
                p.getId(),
                p.getCarcaca(),
                p.getMedida_pneu_raspado(),
                p.getRegra(),
                p.getFotos(),
                p.getDt_create(),
                p.getCriadoPor() != null ? p.getCriadoPor().getNome() : null,
                p.getCriadoPor() != null ? p.getCriadoPor().getLogin() : null
        );
    }
}
