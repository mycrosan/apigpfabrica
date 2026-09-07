package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.leitura.repository.ImagemLeituraRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "pneus.evidencias.reconciliacao-habilitada", havingValue = "true")
public class ReconciliacaoLeituraService {
    private final TentativaPersistenciaService tentativas;
    private final ImagemLeituraRepository imagens;
    private final ArquivosLeituraService arquivos;
    private final Clock relogio;
    @Scheduled(fixedDelayString = "${pneus.evidencias.reconciliacao-intervalo-ms:60000}")
    public void reconciliar() {
        // Cinco minutos excedem o timeout máximo do OCR. Não repetir uma inferência incerta.
        int recuperadas = tentativas.recuperarInterrompidas(relogio.instant().minusSeconds(300));
        for (String arquivo : arquivos.listarAnteriores(relogio.instant().minusSeconds(86400))) {
            if (!imagens.existsByArquivo(arquivo)) {
                arquivos.removerOrfao(arquivo);
                log.info("Evidência órfã removida arquivo={}", arquivo);
            }
        }
        if (recuperadas > 0) { log.info("Tentativas interrompidas recuperadas quantidade={}", recuperadas); }
    }
}
