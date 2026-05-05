package br.compneusgppremium.api.mapper;

import br.compneusgppremium.api.controller.dto.MotoristaCreateDTO;
import br.compneusgppremium.api.controller.dto.MotoristaResponseDTO;
import br.compneusgppremium.api.controller.dto.MotoristaUpdateDTO;
import br.compneusgppremium.api.controller.model.MotoristaModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MotoristaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", source = "usuario")
    @Mapping(target = "nome", source = "dto.nome")
    @Mapping(target = "cpf", source = "dto.cpf")
    @Mapping(target = "telefone", source = "dto.telefone")
    @Mapping(target = "placaVeiculo", source = "dto.placaVeiculo")
    @Mapping(target = "observacoes", source = "dto.observacoes")
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "dataCriacao", expression = "java(new java.util.Date())")
    @Mapping(target = "dataAtualizacao", ignore = true)
    MotoristaModel toEntity(MotoristaCreateDTO dto, UsuarioModel usuario);

    @Mapping(target = "usuarioId", source = "usuario.id")
    MotoristaResponseDTO toDto(MotoristaModel model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget MotoristaModel target, MotoristaUpdateDTO dto);
}
