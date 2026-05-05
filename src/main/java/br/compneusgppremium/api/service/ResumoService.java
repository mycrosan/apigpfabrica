package br.compneusgppremium.api.service;

import br.compneusgppremium.api.util.ResumoEntidade;
import br.compneusgppremium.api.repository.ResumoRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
@Service
public class ResumoService {

    private final ResumoRepository repository;

    public ResumoService(ResumoRepository repository) {
        this.repository = repository;
    }

    public Map<String, Long> getResumo(String entidade) {
        try {
            ResumoEntidade resumoEntidade = ResumoEntidade.valueOf(entidade.toUpperCase());
            return repository.getResumo(resumoEntidade);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Entidade inválida: " + entidade);
        }
    }
}
