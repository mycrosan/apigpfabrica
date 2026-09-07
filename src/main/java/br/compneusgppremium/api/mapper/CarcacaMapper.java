package br.compneusgppremium.api.mapper;
import br.compneusgppremium.api.controller.dto.CarcacaEntradaDTO;
import br.compneusgppremium.api.controller.dto.CarcacaRespostaDTO;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CarcacaMapper {
    CarcacaRespostaDTO resposta(CarcacaModel entidade);
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "numero_etiqueta", source = "numero_etiqueta")
    @Mapping(target = "dot", source = "dot")
    void atualizar(CarcacaEntradaDTO entrada, @MappingTarget CarcacaModel entidade);
}
