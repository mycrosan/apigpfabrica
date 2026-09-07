package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.leitura.repository.TentativaLeituraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
/** Coleta posterior ao cadastro: falhas na preparação da revisão não interrompem o operador. */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "pneus.revisao.coleta-habilitada", havingValue = "true", matchIfMissing = true)
public class ColetaRevisaoService {
    private final TentativaLeituraRepository tentativas;
    private final EnfileirarRevisaoService fila;
    @Scheduled(fixedDelayString = "${pneus.revisao.intervalo-ms:60000}", initialDelayString = "${pneus.revisao.intervalo-ms:60000}")
    public void coletar() {
        for (String tentativa : tentativas.pendentesRevisao(PageRequest.of(0, 20))) {
            try { fila.tentativa(tentativa); }
            catch (RuntimeException erro) {
                log.warn("Preparação de revisão será retomada tentativa={} tipo={}", tentativa, erro.getClass().getSimpleName());
            }
        }
    }
}
