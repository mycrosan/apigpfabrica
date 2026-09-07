package br.compneusgppremium.api.revisao.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "revisao_item")
@Data
public class ItemRevisao {
    @Id @Column(columnDefinition = "char(36)")
    private String id;
    @Column(unique = true, columnDefinition = "char(64)")
    private String chaveOrigem;
    @Column(columnDefinition = "char(36)")
    private String imagemId;
    @Column(length = 24)
    private String origem;
    @Column(columnDefinition = "char(36)")
    private String sessaoId;
    @Column(columnDefinition = "char(36)")
    private String tentativaId;
    @Column(columnDefinition = "char(36)")
    private String confirmacaoId;
    private Integer operadorId;
    @Column(length = 10)
    private String campoSolicitado;
    private Integer marcaId;
    private Integer modeloId;
    @Lob @Column(columnDefinition = "longtext")
    private String regiaoJson;
    @Lob @Column(columnDefinition = "longtext")
    private String evidenciaJson;
    private String valorCadastro;
    private boolean correcao;
    private boolean ambiguidade;
    @Column(length = 20)
    private String estado;
    private boolean reavaliar;
    private boolean pendenciaCadastro;
    private int ciclo;
    @Column(columnDefinition = "char(36)")
    private String ultimaRespostaId;
    @Version
    private Long versao;
    private java.time.Instant criadaEm;
}
