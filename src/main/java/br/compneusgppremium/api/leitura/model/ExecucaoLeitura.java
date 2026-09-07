package br.compneusgppremium.api.leitura.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "leitura_execucao")
@Data
public class ExecucaoLeitura {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String tentativaId;
    @Lob @Column(nullable = false, columnDefinition = "longtext")
    private String snapshotJson;
    @Column(nullable = false, length = 64, columnDefinition = "char(64)")
    private String hashCatalogo;
    @Column(nullable = false)
    private java.time.Instant criadaEm;
}
