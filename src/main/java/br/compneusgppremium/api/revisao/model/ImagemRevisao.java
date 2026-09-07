package br.compneusgppremium.api.revisao.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "revisao_imagem")
@Data
public class ImagemRevisao {
    @Id @Column(columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, unique = true, columnDefinition = "char(64)")
    private String sha256;
    private String arquivo;
    private String previa;
    @Column(columnDefinition = "char(64)")
    private String hashPrevia;
    private Integer largura;
    private Integer altura;
    private java.time.Instant criadaEm;
}
