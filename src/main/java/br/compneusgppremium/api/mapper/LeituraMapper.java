package br.compneusgppremium.api.mapper;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.leitura.dto.CampoConfirmadoDTO;
import br.compneusgppremium.api.leitura.dto.SessaoLeituraDTO;
import br.compneusgppremium.api.leitura.dto.TentativaLeituraDTO;
import br.compneusgppremium.api.leitura.model.SessaoLeitura;
import br.compneusgppremium.api.leitura.model.TentativaLeitura;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeituraMapper {
    br.compneusgppremium.api.leitura.dto.ItemCatalogoLeituraDTO item(Integer id, String descricao);
    SessaoLeituraDTO sessao(SessaoLeitura sessao, Map<String, CampoConfirmadoDTO> confirmacoes,
            List<TentativaLeituraDTO> tentativas);
    @Mapping(target = "tentativaId", source = "tentativa.id")
    @Mapping(target = "campo", source = "tentativa.campo")
    @Mapping(target = "estado", source = "tentativa.estado")
    TentativaLeituraDTO tentativa(TentativaLeitura tentativa, ResultadoLeituraDTO resultado);
}
