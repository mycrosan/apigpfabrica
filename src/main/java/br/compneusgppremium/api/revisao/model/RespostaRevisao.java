package br.compneusgppremium.api.revisao.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "revisao_resposta")
@Data
public class RespostaRevisao {
    @Id @Column(columnDefinition = "char(36)")
    private String id;
    @Column(columnDefinition = "char(36)")
    private String itemId;
    private Integer revisorId;
    @Column(columnDefinition = "char(36)")
    private String chave;
    @Column(columnDefinition = "char(64)")
    private String hashRequisicao;
    private int ciclo;
    @Column(length = 10)
    private String campo;
    @Column(length = 512)
    private String transcricao;
    @Column(length = 24)
    private String legibilidade;
    @Lob @Column(columnDefinition = "longtext")
    private String regiaoJson;
    private boolean adjudicacao;
    @Column(length = 1024)
    private String motivo;
    private java.time.Instant criadaEm;
}
