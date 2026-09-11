package br.compneusgppremium.api.service;

import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.RegraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CombinacaoPneuServiceTest {
    @Test
    void informaQuantidadeMesmoQuandoExisteRegraDeProducao() {
        var carcasas = mock(CarcacaRepository.class);
        var regras = mock(RegraRepository.class);
        var servico = new CombinacaoPneuService();
        ReflectionTestUtils.setField(servico, "carcacaRepository", carcasas);
        ReflectionTestUtils.setField(servico, "regraRepository", regras);
        when(carcasas.countPorTrio(1, 2, 3)).thenReturn(7L);
        when(regras.existeParaTrio(1, 2, 3)).thenReturn(true);
        var resultado = servico.classificar(1, 2, 3);
        assertThat(resultado.getQuantidadeCadastrada()).isEqualTo(7L);
        assertThat(resultado.getClassificacao()).isEqualTo("VERDE");
        verify(carcasas).countPorTrio(1, 2, 3);
    }
}
