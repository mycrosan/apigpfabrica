package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.RegraModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
@Schema(description = "DTO para criação e atualização de produção")
public class ProducaoDTO {
    
    @Schema(description = "ID único da produção", example = "1")
    private Integer id;
    
    @Schema(description = "Dados da carcaça associada à produção")
    private CarcacaModel carcaca;
    
    @Schema(description = "Medida do pneu raspado", example = "15.5")
    private Double medida_pneu_raspado;
    
    @Schema(description = "Dados adicionais da produção em formato string")
    private String dados;
    
    @Schema(description = "Regra aplicada na produção")
    private RegraModel regra;
    
    @Schema(description = "URLs das fotos da produção", example = "foto1.jpg,foto2.jpg")
    private String fotos;
    
    @Schema(description = "Data de criação da produção", example = "2024-01-15T10:30:00.000Z")
    private Date dt_create;
    
    @Schema(description = "Data de última atualização da produção", example = "2024-01-15T10:30:00.000Z")
    private Date dt_update;
    
    @Schema(description = "UUID único da produção", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;
    
    @Schema(description = "Dados resumidos do usuário que criou a produção")
    private UsuarioResumoDTO criadoPor;

    public ProducaoDTO(Integer id, CarcacaModel carcaca, Double medida_pneu_raspado,
                       RegraModel regra, String fotos, Date dt_create,
                       String nome, String login) {
        this.id = id;
        this.carcaca = carcaca;
        this.medida_pneu_raspado = medida_pneu_raspado;
        this.regra = regra;
        this.fotos = fotos;
        this.dt_create = dt_create;
        this.criadoPor = new UsuarioResumoDTO(nome, login);
    }

    // ✅ Factory method (certifique-se de que é `public static`)
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
