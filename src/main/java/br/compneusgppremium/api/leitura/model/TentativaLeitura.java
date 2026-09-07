package br.compneusgppremium.api.leitura.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "leitura_tentativa")
@Data
public class TentativaLeitura {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String sessaoId;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String imagemId;
    @Column(nullable = false, length = 128)
    private String chaveIdempotencia;
    @Column(nullable = false, length = 64, columnDefinition = "char(64)")
    private String hashRequisicao;
    @Column(nullable = false, length = 10)
    private String campo;
    @Lob @Column(nullable = false, columnDefinition = "longtext")
    private String contextoJson;
    @Column(nullable = false)
    private Long versaoSessao;
    @Column(nullable = false, length = 30)
    private String estado;
    @Lob @Column(columnDefinition = "longtext")
    private String respostaJson;
    @Column(nullable = false)
    private java.time.Instant criadaEm;
}
