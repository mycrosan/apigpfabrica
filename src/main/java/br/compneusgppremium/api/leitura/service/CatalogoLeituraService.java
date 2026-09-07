package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.leitura.dto.ItemCatalogoLeituraDTO;
import br.compneusgppremium.api.mapper.LeituraMapper;
import br.compneusgppremium.api.service.ResolverCatalogoPneuService;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoLeituraService {
    private final ResolverCatalogoPneuService catalogo;
    private final LeituraMapper mapper;
    public List<ItemCatalogoLeituraDTO> listar(final Campo campo, final Integer marcaId) {
        return catalogo.snapshot(campo.name(), marcaId, null).entrySet().stream()
                .map(item -> mapper.item(item.getKey(), item.getValue()))
                .sorted(java.util.Comparator.comparing(ItemCatalogoLeituraDTO::descricao)).toList();
    }
}
