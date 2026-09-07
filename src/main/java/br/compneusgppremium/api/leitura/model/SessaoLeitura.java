package br.compneusgppremium.api.leitura.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "leitura_sessao")
@Data
public class SessaoLeitura {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false)
    private Integer operadorId;
    
    private Integer carcacaId;
    private Integer carcacaInicialId;
    @Column(length = 1024)
    private String motivoCombinacaoNova;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String grupoFisico;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private java.time.Instant criadaEm;
    @Version
    private Long versao;
    @Lob @Column(nullable = false, columnDefinition = "longtext")
    private String confirmacoesJson;
    @Column(length = 1024)
    private String motivoAbandono;
    private java.time.Instant abandonadaEm;
    private Integer abandonadaPor;
    @Column(length = 128)
    private String chaveCadastro;
    @Column(length = 64, columnDefinition = "char(64)")
    private String hashCadastro;
}
