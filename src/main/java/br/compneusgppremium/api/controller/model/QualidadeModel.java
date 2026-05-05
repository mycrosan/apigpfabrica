package br.compneusgppremium.api.controller.model;

import br.compneusgppremium.api.util.JpaConverterJson;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "controle_qualidade")
@Data
@Service
@Schema(description = "Modelo representando o controle de qualidade de pneus")
public class QualidadeModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do controle de qualidade", example = "1")
    private Integer id;
    
    @ManyToOne
    @Schema(description = "Produção associada ao controle de qualidade")
    private ProducaoModel producao;
    
    @Column
    @Schema(description = "Observação sobre a qualidade", example = "Pneu aprovado sem defeitos")
    private String observacao;
    
    @Column
    @Convert(converter = JpaConverterJson.class)
    @Schema(description = "URLs das fotos do controle de qualidade em formato JSON")
    public String fotos;
    
    @ManyToOne
    @Schema(description = "Tipo de observação do controle de qualidade")
    private TipoObservacaoModel tipo_observacao;
    
    @Column
    @Schema(description = "Data de criação do controle de qualidade")
    private Date dt_create;
}
