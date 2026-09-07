package br.compneusgppremium.api.revisao.mapper;
import br.compneusgppremium.api.revisao.dto.*;
import br.compneusgppremium.api.revisao.model.*;
import org.mapstruct.*;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RevisaoMapper {
    RespostaRevisaoDTO resposta(RespostaRevisao entidade);
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "campo", source = "campo")
    @Mapping(target = "transcricao", source = "transcricao")
    @Mapping(target = "legibilidade", source = "legibilidade")
    @Mapping(target = "motivo", source = "motivo")
    RespostaRevisao entrada(RevisarEntradaDTO entrada);
}
