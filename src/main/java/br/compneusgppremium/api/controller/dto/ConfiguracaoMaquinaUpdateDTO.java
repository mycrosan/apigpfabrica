package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Size;

/**
 * DTO para atualização de configuração de máquina
 */
@Schema(description = "Dados para atualização de configuração de máquina")
public class ConfiguracaoMaquinaUpdateDTO {

    @Size(max = 100, message = "ID do celular deve ter no máximo 100 caracteres")
    @Schema(description = "ID do celular", example = "CEL001")
    private String celularId;

    @Schema(description = "Descrição da configuração", example = "Configuração de produção")
    private String descricao;

    @Schema(description = "Atributos da configuração em formato JSON", example = "{\"velocidade\": 100, \"temperatura\": 80}")
    private String atributos;

    @Schema(description = "ID da matriz", example = "1")
    private Integer matrizId;

    // Construtores
    public ConfiguracaoMaquinaUpdateDTO() {}

    public ConfiguracaoMaquinaUpdateDTO(String celularId, String descricao, 
                                       String atributos, Integer matrizId) {
        this.celularId = celularId;
        this.descricao = descricao;
        this.atributos = atributos;
        this.matrizId = matrizId;
    }

    // Getters e Setters
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

    public Integer getMatrizId() {
        return matrizId;
    }

    public void setMatrizId(Integer matrizId) {
        this.matrizId = matrizId;
    }
}