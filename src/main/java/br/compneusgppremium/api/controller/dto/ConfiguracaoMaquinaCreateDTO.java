package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTO para criação de configuração de máquina
 */
@Schema(description = "Dados para criação de configuração de máquina")
public class ConfiguracaoMaquinaCreateDTO {

    @NotNull(message = "ID da máquina é obrigatório")
    @Schema(description = "ID da máquina", example = "1", required = true)
    private Long maquinaId;

    @NotNull(message = "ID da matriz é obrigatório")
    @Schema(description = "ID da matriz", example = "1", required = true)
    private Integer matrizId;

    @NotBlank(message = "ID do celular é obrigatório")
    @Size(max = 100, message = "ID do celular deve ter no máximo 100 caracteres")
    @Schema(description = "ID do celular", example = "CEL001", required = true)
    private String celularId;

    @Schema(description = "Descrição da configuração", example = "Configuração de produção")
    private String descricao;

    @Schema(description = "Atributos da configuração em formato JSON", example = "{\"velocidade\": 100, \"temperatura\": 80}")
    private String atributos;

    // Construtores
    public ConfiguracaoMaquinaCreateDTO() {}

    public ConfiguracaoMaquinaCreateDTO(Long maquinaId, Integer matrizId, String celularId, 
                                       String descricao, String atributos) {
        this.maquinaId = maquinaId;
        this.matrizId = matrizId;
        this.celularId = celularId;
        this.descricao = descricao;
        this.atributos = atributos;
    }

    // Getters e Setters
    public Long getMaquinaId() {
        return maquinaId;
    }

    public void setMaquinaId(Long maquinaId) {
        this.maquinaId = maquinaId;
    }

    public Integer getMatrizId() {
        return matrizId;
    }

    public void setMatrizId(Integer matrizId) {
        this.matrizId = matrizId;
    }

    public String getCelularId() {
        return celularId;
    }

    public void setCelularId(String celularId) {
        this.celularId = celularId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getAtributos() {
        return atributos;
    }

    public void setAtributos(String atributos) {
        this.atributos = atributos;
    }
}