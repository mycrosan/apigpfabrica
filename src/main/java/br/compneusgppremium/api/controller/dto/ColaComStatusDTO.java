package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.CoberturaModel;
import br.compneusgppremium.api.controller.model.ColaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO contendo informações da cola com status de validação")
public class ColaComStatusDTO {
    @Schema(description = "Dados da cola")
    private ColaModel cola;
    
    @Schema(description = "Dados da produção associada")
    private ProducaoModel producao;
    
    @Schema(description = "Indica se a cola é válida", example = "true")
    private boolean colaValida;
    
    @Schema(description = "Mensagem de status da validação", example = "Cola válida para cobertura")
    private String mensagem;
    
    @Schema(description = "Dados da cobertura, se existir")
    private CoberturaModel cobertura;
    
    @Schema(description = "Usuário responsável")
    private UsuarioModel usuario;

    public ColaComStatusDTO(ColaModel cola,
                            ProducaoModel producao,
                            boolean colaValida,
                            String mensagem,
                            UsuarioModel usuarioLogado) {
        this.cola = cola;
        this.producao = producao;
        this.colaValida = colaValida;
        this.mensagem = mensagem;
        this.usuario = usuarioLogado;
    }

    // getters e setters

    public ColaModel getCola() { return cola; }
    public void setCola(ColaModel cola) { this.cola = cola; }

    public ProducaoModel getProducao() { return producao; }
    public void setProducao(ProducaoModel producao) { this.producao = producao; }

    public boolean isColaValida() { return colaValida; }
    public void setColaValida(boolean colaValida) { this.colaValida = colaValida; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public CoberturaModel getCobertura() { return cobertura; }
    public void setCobertura(CoberturaModel cobertura) { this.cobertura = cobertura; }

    public UsuarioModel getUsuario() { return usuario; }   // ✅ corrigido
    public void setUsuario(UsuarioModel usuario) { this.usuario = usuario; } // ✅ corrigido
}
