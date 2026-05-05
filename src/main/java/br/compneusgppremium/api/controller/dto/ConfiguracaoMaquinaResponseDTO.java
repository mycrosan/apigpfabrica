package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO para resposta de configuração de máquina
 */
@Schema(description = "Dados de resposta de configuração de máquina")
public class ConfiguracaoMaquinaResponseDTO {

    @Schema(description = "ID da configuração", example = "1")
    private Long id;

    @Schema(description = "ID do celular", example = "CEL001")
    private String celularId;

    @Schema(description = "Descrição da configuração", example = "Configuração de produção")
    private String descricao;

    @Schema(description = "Atributos da configuração em formato JSON", example = "{\"velocidade\": 100, \"temperatura\": 80}")
    private String atributos;

    @Schema(description = "ID da matriz", example = "1")
    private Integer matrizId;

    @Schema(description = "Nome da matriz", example = "Matriz Principal")
    private String matrizNome;

    @Schema(description = "ID da máquina", example = "1")
    private Long maquinaId;

    @Schema(description = "Nome da máquina", example = "Máquina de Produção 01")
    private String maquinaNome;

    @Schema(description = "Data de criação", example = "2024-01-15T10:30:00")
    private LocalDateTime dtCreate;

    @Schema(description = "Data de atualização", example = "2024-01-15T14:30:00")
    private LocalDateTime dtUpdate;

    @Schema(description = "ID do usuário que criou a configuração", example = "1")
    private Long usuarioId;

    // Construtores
    public ConfiguracaoMaquinaResponseDTO() {}

    public ConfiguracaoMaquinaResponseDTO(Long id, String celularId, String descricao,
                                         String atributos, Integer matrizId, String matrizNome,
                                         Long maquinaId, String maquinaNome,
                                         LocalDateTime dtCreate, LocalDateTime dtUpdate, Long usuarioId) {
        this.id = id;
        this.celularId = celularId;
        this.descricao = descricao;
        this.atributos = atributos;
        this.matrizId = matrizId;
        this.matrizNome = matrizNome;
        this.maquinaId = maquinaId;
        this.maquinaNome = maquinaNome;
        this.dtCreate = dtCreate;
        this.dtUpdate = dtUpdate;
        this.usuarioId = usuarioId;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getMatrizId() {
        return matrizId;
    }

    public void setMatrizId(Integer matrizId) {
        this.matrizId = matrizId;
    }

    public String getMatrizNome() {
        return matrizNome;
    }

    public void setMatrizNome(String matrizNome) {
        this.matrizNome = matrizNome;
    }

    public Long getMaquinaId() {
        return maquinaId;
    }

    public void setMaquinaId(Long maquinaId) {
        this.maquinaId = maquinaId;
    }

    public String getMaquinaNome() {
        return maquinaNome;
    }

    public void setMaquinaNome(String maquinaNome) {
        this.maquinaNome = maquinaNome;
    }

    public LocalDateTime getDtCreate() {
        return dtCreate;
    }

    public void setDtCreate(LocalDateTime dtCreate) {
        this.dtCreate = dtCreate;
    }

    public LocalDateTime getDtUpdate() {
        return dtUpdate;
    }

    public void setDtUpdate(LocalDateTime dtUpdate) {
        this.dtUpdate = dtUpdate;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}