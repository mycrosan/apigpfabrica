package br.compneusgppremium.api.integration;

import br.compneusgppremium.api.controller.ProducaoController;
import br.compneusgppremium.api.controller.QualidadeController;
import br.compneusgppremium.api.controller.enums.RegraStatus;
import br.compneusgppremium.api.controller.model.*;
import br.compneusgppremium.api.repository.*;
import br.compneusgppremium.api.util.ApiError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FluxoValidacaoRegraIT {

    @Autowired private ProducaoController producaoController;
    @Autowired private QualidadeController qualidadeController;

    @Autowired private RegraRepository regraRepository;
    @Autowired private ProducaoRepository producaoRepository;
    @Autowired private QualidadeRepository qualidadeRepository;
    @Autowired private CarcacaRepository carcacaRepository;
    @Autowired private MedidaRepository medidaRepository;
    @Autowired private ModeloRepository modeloRepository;
    @Autowired private PaisRepository paisRepository;
    @Autowired private MatrizRepository matrizRepository;
    @Autowired private TipoObservacaoRepository tipoObservacaoRepository;
    @Autowired private TipoClassificacaoRepository tipoClassificacaoRepository;
    @Autowired private ValidaRegraRepository validaRegraRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private void autenticarUsuarioTeste() {
        UsuarioModel usuario = usuarioRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Usuário de teste não encontrado"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void fluxoCompleto_validacaoDeRegra() {
        autenticarUsuarioTeste();

        // 1) Criar entidades base
        MatrizModel matriz = new MatrizModel();
        matriz.setDescricao("Matriz IT");
        matriz = matrizRepository.save(matriz);

        MedidaModel medida = new MedidaModel();
        medida.descricao = "205/55R16";
        medida = medidaRepository.save(medida);

        ModeloModel modelo = new ModeloModel();
        modelo.setDescricao("Modelo IT");
        modelo = modeloRepository.save(modelo);

        PaisModel pais = new PaisModel();
        pais.setDescricao("Brasil");
        pais = paisRepository.save(pais);

        // 2) Criar regra EM_VALIDACAO
        RegraModel regra = new RegraModel();
        regra.setMatriz(matriz);
        regra.setMedida(medida);
        regra.setModelo(modelo);
        regra.setPais(pais);
        regra.setTamanho_min(15.0);
        regra.setTamanho_max(22.0);
        regra.setTempo("120");
        regra.setDt_create(new Date());
        regra.setStatus(RegraStatus.EM_VALIDACAO);
        regra = regraRepository.save(regra);

        // 3) Criar carcaça
        CarcacaModel carcaca = new CarcacaModel();
        carcaca.setNumero_etiqueta("ETQ-IT-0001");
        carcaca.setStatus("start");
        carcaca.setDt_create(new Date());
        carcaca = carcacaRepository.save(carcaca);

        // 4) Criar primeira produção (deve permitir)
        ProducaoModel producao1 = new ProducaoModel();
        producao1.setCarcaca(carcaca);
        producao1.setMedida_pneu_raspado(16.0);
        producao1.setRegra(regra);
        Object respProducao1 = producaoController.salvar(producao1);
        assertFalse(respProducao1 instanceof ApiError, "Primeira produção deve ser criada mesmo com regra EM_VALIDACAO");
        assertTrue(respProducao1 instanceof ProducaoModel);
        producao1 = (ProducaoModel) respProducao1;
        assertNotNull(producao1.getId(), "Produção 1 deve ter ID após persistência");

        // 5) Qualidade aprovada para a produção 1
        TipoObservacaoModel tipoObs = tipoObservacaoRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("TipoObservacao id=1 (APROVADO) não encontrado no seed"));
        QualidadeModel qualidade = new QualidadeModel();
        qualidade.setProducao(producao1);
        qualidade.setObservacao("Qualidade OK");
        qualidade.setTipo_observacao(tipoObs);
        Object respQualidade = qualidadeController.salvar(qualidade);
        if (respQualidade instanceof ApiError) {
            ApiError err = (ApiError) respQualidade;
            fail("Qualidade retornou ApiError: status=" + err.getStatus() + ", message=" + err.getMessage() + ", debug=" + err.getDebugMessage());
        }
        QualidadeModel qualidadeSalva;
        if (respQualidade instanceof java.util.Optional) {
            @SuppressWarnings("unchecked")
            java.util.Optional<QualidadeModel> opt = (java.util.Optional<QualidadeModel>) respQualidade;
            assertTrue(opt.isPresent(), "Qualidade deve ser salva e presente no Optional");
            qualidadeSalva = opt.get();
        } else {
            assertTrue(respQualidade instanceof QualidadeModel, "Retorno deve ser QualidadeModel");
            qualidadeSalva = (QualidadeModel) respQualidade;
        }

        // 6) Verificar que a regra foi VALIDADA e há validação registrada
        RegraModel regraAtualizada = regraRepository.findById(regra.getId())
                .orElseThrow(() -> new RuntimeException("Regra não encontrada após atualização"));
        assertEquals(RegraStatus.VALIDADA, regraAtualizada.getStatus(), "Regra deve estar VALIDADA após qualidade aprovada");
        long totalAprovadas = validaRegraRepository.countAprovadasByRegraId(regra.getId());
        assertEquals(1L, totalAprovadas, "Deve haver uma validação aprovada para a regra");

        // 7) Criar segunda produção (agora deve permitir pois regra está VALIDADA)
        CarcacaModel carcaca2 = new CarcacaModel();
        carcaca2.setNumero_etiqueta("ETQ-IT-0002");
        carcaca2.setStatus("start");
        carcaca2.setDt_create(new Date());
        carcaca2 = carcacaRepository.save(carcaca2);

        ProducaoModel producao2 = new ProducaoModel();
        producao2.setCarcaca(carcaca2);
        producao2.setMedida_pneu_raspado(17.0);
        producao2.setRegra(regraAtualizada);
        Object respProducao2 = producaoController.salvar(producao2);
        assertFalse(respProducao2 instanceof ApiError, "Segunda produção deve ser permitida quando regra está VALIDADA");
        assertTrue(respProducao2 instanceof ProducaoModel);

        long contagemProducoesRegra = producaoRepository.countByRegraId(regra.getId());
        assertTrue(contagemProducoesRegra >= 2, "Regra deve ter pelo menos duas produções após validação");
    }
}
