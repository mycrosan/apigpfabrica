package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.QualidadeModel;
import br.compneusgppremium.api.controller.model.TipoObservacaoModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "DTO para retorno de controle de qualidade")
public class QualidadeDTO {

    @Schema(description = "ID único do controle de qualidade", example = "1")
    private Integer id;

    @Schema(description = "Produção associada ao controle de qualidade")
    private ProducaoModel producao;

    @Schema(description = "Observação sobre a qualidade", example = "Pneu aprovado sem defeitos")
    private String observacao;

    @Schema(description = "URLs das fotos do controle de qualidade em formato JSON")
    private String fotos;

    @Schema(description = "Tipo de observação do controle de qualidade")
    private TipoObservacaoModel tipo_observacao;

    @Schema(description = "Data de criação do controle de qualidade")
    private Date dt_create;

    @Schema(description = "Usuário responsável pela classificação (pode ser null para registros antigos)")
    private UsuarioResponsavelDTO usuario;

    public static QualidadeDTO fromModel(QualidadeModel qualidade) {
        QualidadeDTO dto = new QualidadeDTO();
        dto.setId(qualidade.getId());
        dto.setProducao(qualidade.getProducao());
        dto.setObservacao(qualidade.getObservacao());
        dto.setFotos(qualidade.getFotos());
        dto.setTipo_observacao(qualidade.getTipo_observacao());
        dto.setDt_create(qualidade.getDt_create());
        dto.setUsuario(toUsuario(qualidade));
        return dto;
    }

    private static UsuarioResponsavelDTO toUsuario(QualidadeModel qualidade) {
        if (qualidade.getUsuario() == null) {
            return null;
        }

        return new UsuarioResponsavelDTO(
                qualidade.getUsuario().getId(),
                qualidade.getUsuario().getNome(),
                qualidade.getUsuario().getLogin()
        );
    }
}
