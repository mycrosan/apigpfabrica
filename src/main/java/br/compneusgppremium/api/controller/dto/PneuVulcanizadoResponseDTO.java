package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel.StatusVulcanizacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de resposta para pneu vulcanizado
 */
@Schema(description = "Dados de resposta de pneu vulcanizado")
public class PneuVulcanizadoResponseDTO {

    @Schema(description = "ID único do pneu vulcanizado", example = "1")
    private Long id;

    @Schema(description = "ID do usuário responsável", example = "1")
    private Long usuarioId;

    @Schema(description = "Nome do usuário responsável", example = "João Silva")
    private String usuarioNome;

    @Schema(description = "ID da produção relacionada", example = "1")
    private Integer producaoId;

    @Schema(description = "Número da etiqueta da carcaça", example = "CAR001")
    private String numeroEtiqueta;

    @Schema(description = "Status da vulcanização", example = "INICIADO")
    private StatusVulcanizacao status;

    @Schema(description = "Data e hora de criação")
    private LocalDateTime dtCreate;

    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;

    // Construtores
    public PneuVulcanizadoResponseDTO() {}

    public PneuVulcanizadoResponseDTO(Long id, Long usuarioId, String usuarioNome, 
                                     Integer producaoId, String numeroEtiqueta, StatusVulcanizacao status, 
                                     LocalDateTime dtCreate, LocalDateTime dtUpdate) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.producaoId = producaoId;
        this.numeroEtiqueta = numeroEtiqueta;
        this.status = status;
        this.dtCreate = dtCreate;
        this.dtUpdate = dtUpdate;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public Integer getProducaoId() {
        return producaoId;
    }

    public void setProducaoId(Integer producaoId) {
        this.producaoId = producaoId;
    }

    public String getNumeroEtiqueta() {
        return numeroEtiqueta;
    }

    public void setNumeroEtiqueta(String numeroEtiqueta) {
        this.numeroEtiqueta = numeroEtiqueta;
    }

    public StatusVulcanizacao getStatus() {
        return status;
    }

    public void setStatus(StatusVulcanizacao status) {
        this.status = status;
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
}