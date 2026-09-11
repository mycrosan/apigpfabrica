package br.compneusgppremium.api.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JpaConverterJsonTest {
    private final JpaConverterJson conversor = new JpaConverterJson();

    @Test
    void permiteLerCamposOpcionaisNulosDeCadastrosLegados() {
        assertThat(conversor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void preservaLeituraDeNullJsonJaGravado() {
        assertThat(conversor.convertToEntityAttribute("null")).isNull();
    }

    @Test
    void preservaFormatoDeTextoJsonJaGravado() {
        String fotos = "[\"foto.jpg\"]";
        String persistido = conversor.convertToDatabaseColumn(fotos);
        assertThat(persistido).isEqualTo("\"[\\\"foto.jpg\\\"]\"");
        assertThat(conversor.convertToEntityAttribute(persistido)).isEqualTo(fotos);
    }
}
