package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.dto.MotoristaCreateDTO;
import br.compneusgppremium.api.controller.dto.MotoristaResponseDTO;
import br.compneusgppremium.api.controller.dto.MotoristaUpdateDTO;
import br.compneusgppremium.api.controller.model.MotoristaModel;
import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.exception.BusinessException;
import br.compneusgppremium.api.exception.NotFoundException;
import br.compneusgppremium.api.mapper.MotoristaMapper;
import br.compneusgppremium.api.repository.AuditoriaRepository;
import br.compneusgppremium.api.repository.MotoristaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotoristaMapper motoristaMapper;
    private final AuditoriaService auditoriaService;
    private final AuditoriaRepository auditoriaRepository;

    @Transactional
    public MotoristaResponseDTO criar(MotoristaCreateDTO dto) {
        if (motoristaRepository.existsByCpf(dto.getCpf())) {
            throw new BusinessException("CPF já cadastrado");
        }
        Optional<UsuarioModel> usuarioOpt = usuarioRepository.findById(dto.getUsuarioId().longValue());
        if (!usuarioOpt.isPresent()) {
            throw new NotFoundException("Usuário não encontrado");
        }
        UsuarioModel usuario = usuarioOpt.get();
        validarPerfilExclusivoMotorista(usuario);

        if (motoristaRepository.existsByUsuarioId(dto.getUsuarioId().longValue())) {
            throw new BusinessException("Usuário já vinculado a um motorista");
        }

        MotoristaModel entity = motoristaMapper.toEntity(dto, usuario);
        MotoristaModel saved = motoristaRepository.save(entity);

        auditoriaService.registrar("motorista", saved.getId().longValue(), "INSERT", usuario);
        return motoristaMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MotoristaResponseDTO> listar(Boolean ativo) {
        return motoristaRepository.findAllByAtivo(ativo)
                .stream().map(motoristaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MotoristaResponseDTO consultar(Integer id) {
        MotoristaModel m = motoristaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Motorista não encontrado"));
        return motoristaMapper.toDto(m);
    }

    @Transactional
    public MotoristaResponseDTO atualizar(Integer id, MotoristaUpdateDTO dto) {
        MotoristaModel m = motoristaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Motorista não encontrado"));
        motoristaMapper.update(m, dto);
        m.setDataAtualizacao(new java.util.Date());
        MotoristaModel saved = motoristaRepository.save(m);
        auditoriaService.registrar("motorista", saved.getId().longValue(), "UPDATE", saved.getUsuario());
        return motoristaMapper.toDto(saved);
    }

    @Transactional
    public void inativar(Integer id) {
        MotoristaModel m = motoristaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Motorista não encontrado"));

        Long usuarioId = m.getUsuario().getId();
        boolean participouCompras = auditoriaRepository.existsByTabelaAfetadaAndUsuario_Id("compra", usuarioId);
        if (participouCompras) {
            throw new BusinessException("Motorista não pode ser inativado: possui compras registradas");
        }

        m.setAtivo(false);
        m.setDataAtualizacao(new java.util.Date());
        motoristaRepository.save(m);
        auditoriaService.registrar("motorista", m.getId().longValue(), "INATIVAR", m.getUsuario());
    }

    private void validarPerfilExclusivoMotorista(UsuarioModel usuario) {
        List<PerfilModel> perfis = usuario.getPerfil();
        boolean possuiMotorista = perfis.stream().anyMatch(p -> "MOTORISTA".equalsIgnoreCase(p.getDescricao()));
        if (!possuiMotorista) {
            throw new BusinessException("Usuário deve possuir perfil MOTORISTA");
        }
        long outrosPerfis = perfis.stream().filter(p -> !"MOTORISTA".equalsIgnoreCase(p.getDescricao())).count();
        if (outrosPerfis > 0) {
            throw new BusinessException("Perfil MOTORISTA não pode acumular outros perfis");
        }
    }
}
