package br.compneusgppremium.api.leitura.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "leitura_imagem")
@Data
public class ImagemLeitura {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String sessaoId;
    @Column(nullable = false)
    private String arquivo;
    @Column(nullable = false, length = 64, columnDefinition = "char(64)")
    private String sha256;
    @Column(nullable = false)
    private Long tamanhoBytes;
    @Column(length = 32)
    private String mime;
    private Integer largura;
    private Integer altura;
    @Column(nullable = false)
    private java.time.Instant recebidaEm;
}
