package br.compneusgppremium.api.leitura.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "leitura_confirmacao")
@Data
public class ConfirmacaoLeitura {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String sessaoId;
    @Column(length = 36, columnDefinition = "char(36)")
    private String tentativaId;
    @Column(nullable = false, length = 10)
    private String campo;
    @Column(nullable = false, length = 20)
    private String origem;
    
    private Integer itemFinalId;
    
    private String valorFinal;
    @Column(length = 1024)
    private String motivo;
    @Column(nullable = false)
    private Integer operadorId;
    @Column(nullable = false)
    private java.time.Instant criadaEm;
    @Column(nullable = false)
    private Long versaoSessao;
}
