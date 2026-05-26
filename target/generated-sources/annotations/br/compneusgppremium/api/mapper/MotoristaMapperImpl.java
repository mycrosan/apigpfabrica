package br.compneusgppremium.api.mapper;

import br.compneusgppremium.api.controller.dto.MotoristaCreateDTO;
import br.compneusgppremium.api.controller.dto.MotoristaResponseDTO;
import br.compneusgppremium.api.controller.dto.MotoristaUpdateDTO;
import br.compneusgppremium.api.controller.model.MotoristaModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-26T20:23:39-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class MotoristaMapperImpl implements MotoristaMapper {

    @Override
    public MotoristaModel toEntity(MotoristaCreateDTO dto, UsuarioModel usuario) {
        if ( dto == null && usuario == null ) {
            return null;
        }

        MotoristaModel motoristaModel = new MotoristaModel();

        if ( dto != null ) {
            motoristaModel.setNome( dto.getNome() );
            motoristaModel.setCpf( dto.getCpf() );
            motoristaModel.setTelefone( dto.getTelefone() );
            motoristaModel.setPlacaVeiculo( dto.getPlacaVeiculo() );
            motoristaModel.setObservacoes( dto.getObservacoes() );
        }
        motoristaModel.setUsuario( usuario );
        motoristaModel.setAtivo( true );
        motoristaModel.setDataCriacao( new java.util.Date() );

        return motoristaModel;
    }

    @Override
    public MotoristaResponseDTO toDto(MotoristaModel model) {
        if ( model == null ) {
            return null;
        }

        Integer usuarioId = null;
        Integer id = null;
        String nome = null;
        String cpf = null;
        String telefone = null;
        String placaVeiculo = null;
        String observacoes = null;
        Boolean ativo = null;

        Long id1 = modelUsuarioId( model );
        if ( id1 != null ) {
            usuarioId = id1.intValue();
        }
        id = model.getId();
        nome = model.getNome();
        cpf = model.getCpf();
        telefone = model.getTelefone();
        placaVeiculo = model.getPlacaVeiculo();
        observacoes = model.getObservacoes();
        ativo = model.getAtivo();

        MotoristaResponseDTO motoristaResponseDTO = new MotoristaResponseDTO( id, nome, cpf, telefone, placaVeiculo, observacoes, usuarioId, ativo );

        return motoristaResponseDTO;
    }

    @Override
    public void update(MotoristaModel target, MotoristaUpdateDTO dto) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getNome() != null ) {
            target.setNome( dto.getNome() );
        }
        if ( dto.getTelefone() != null ) {
            target.setTelefone( dto.getTelefone() );
        }
        if ( dto.getPlacaVeiculo() != null ) {
            target.setPlacaVeiculo( dto.getPlacaVeiculo() );
        }
        if ( dto.getObservacoes() != null ) {
            target.setObservacoes( dto.getObservacoes() );
        }
    }

    private Long modelUsuarioId(MotoristaModel motoristaModel) {
        if ( motoristaModel == null ) {
            return null;
        }
        UsuarioModel usuario = motoristaModel.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        Long id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
