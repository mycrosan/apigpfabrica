package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.dto.PneuVulcanizadoCreateDTO;
import br.compneusgppremium.api.controller.dto.PneuVulcanizadoResponseDTO;
import br.compneusgppremium.api.controller.dto.PneuVulcanizadoUpdateDTO;
import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel;
import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel.StatusVulcanizacao;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.PneuVulcanizadoRepository;
import br.compneusgppremium.api.repository.ProducaoRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PneuVulcanizadoService {

    private final PneuVulcanizadoRepository pneuVulcanizadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProducaoRepository producaoRepository;

    public PneuVulcanizadoService(PneuVulcanizadoRepository pneuVulcanizadoRepository,
                                 UsuarioRepository usuarioRepository,
                                 ProducaoRepository producaoRepository) {
        this.pneuVulcanizadoRepository = pneuVulcanizadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.producaoRepository = producaoRepository;
    }

    /**
     * Cria um novo pneu vulcanizado com status INICIADO
     */
    public PneuVulcanizadoResponseDTO criar(PneuVulcanizadoCreateDTO createDTO, Long usuarioId) {
        // Validar se usuário existe
        UsuarioModel usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + usuarioId));

        // Validar se produção existe
        ProducaoModel producao = producaoRepository.findById(createDTO.getProducaoId())
                .orElseThrow(() -> new RuntimeException("Produção não encontrada com ID: " + createDTO.getProducaoId()));

        // Validar apenas se a produção já foi concluída com sucesso (status FINALIZADO)
        Long finalizados = pneuVulcanizadoRepository
                .countByProducaoIdAndStatus(Long.valueOf(createDTO.getProducaoId()), StatusVulcanizacao.FINALIZADO);
        if (finalizados != null && finalizados > 0) {
            throw new RuntimeException("Produção já realizada. Não pode ser vulcanizada novamente.");
        }

        // Criar novo pneu vulcanizado
        PneuVulcanizadoModel pneuVulcanizado = new PneuVulcanizadoModel();
        pneuVulcanizado.setUsuarioId(usuarioId);
        pneuVulcanizado.setUsuario(usuario);
        // ProducaoModel usa Integer como ID, enquanto PneuVulcanizadoModel armazena Long
        pneuVulcanizado.setProducaoId(Long.valueOf(createDTO.getProducaoId()));
        pneuVulcanizado.setProducao(producao);
        pneuVulcanizado.setStatus(StatusVulcanizacao.INICIADO); // Status padrão
        pneuVulcanizado.setDtCreate(java.time.LocalDateTime.now());

        PneuVulcanizadoModel salvo = pneuVulcanizadoRepository.save(pneuVulcanizado);
        return converterParaResponseDTO(salvo);
    }

    /**
     * Atualiza o status de um pneu vulcanizado
     */
    public PneuVulcanizadoResponseDTO atualizar(Long id, PneuVulcanizadoUpdateDTO updateDTO) {
        PneuVulcanizadoModel pneuVulcanizado = pneuVulcanizadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pneu vulcanizado não encontrado com ID: " + id));

        if (pneuVulcanizado.getDtDelete() != null) {
            throw new RuntimeException("Não é possível atualizar um pneu vulcanizado excluído");
        }

        pneuVulcanizado.setStatus(updateDTO.getStatus());
        PneuVulcanizadoModel salvo = pneuVulcanizadoRepository.save(pneuVulcanizado);
        return converterParaResponseDTO(salvo);
    }

    /**
     * Finaliza um pneu vulcanizado (atualiza status para FINALIZADO)
     */
    public PneuVulcanizadoResponseDTO finalizar(Long id) {
        PneuVulcanizadoUpdateDTO updateDTO = new PneuVulcanizadoUpdateDTO(StatusVulcanizacao.FINALIZADO);
        return atualizar(id, updateDTO);
    }

    /**
     * Busca pneu vulcanizado por ID
     */
    @Transactional(readOnly = true)
    public PneuVulcanizadoResponseDTO buscarPorId(Long id) {
        PneuVulcanizadoModel pneuVulcanizado = pneuVulcanizadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pneu vulcanizado não encontrado com ID: " + id));

        if (pneuVulcanizado.getDtDelete() != null) {
            throw new RuntimeException("Pneu vulcanizado foi excluído");
        }

        return converterParaResponseDTO(pneuVulcanizado);
    }

    /**
     * Lista pneus vulcanizados por usuário
     */
    @Transactional(readOnly = true)
    public List<PneuVulcanizadoResponseDTO> listarPorUsuario(Long usuarioId) {
        List<PneuVulcanizadoModel> pneus = pneuVulcanizadoRepository.findByUsuarioId(usuarioId);
        return pneus.stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista pneus vulcanizados por status
     */
    @Transactional(readOnly = true)
    public List<PneuVulcanizadoResponseDTO> listarPorStatus(StatusVulcanizacao status) {
        List<PneuVulcanizadoModel> pneus = pneuVulcanizadoRepository.findByStatus(status);
        return pneus.stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos os pneus vulcanizados ativos com paginação
     */
    @Transactional(readOnly = true)
    public Page<PneuVulcanizadoResponseDTO> listarTodos(Pageable pageable) {
        Page<PneuVulcanizadoModel> pneus = pneuVulcanizadoRepository.findAllActive(pageable);
        return pneus.map(this::converterParaResponseDTO);
    }

    /**
     * Lista todos os pneus vulcanizados com paginação e filtros
     */
    @Transactional(readOnly = true)
    public Page<PneuVulcanizadoResponseDTO> listarTodos(int page, int size, Long usuarioId, String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PneuVulcanizadoModel> pneus;
        
        if (usuarioId != null && status != null) {
            StatusVulcanizacao statusEnum = StatusVulcanizacao.valueOf(status.toUpperCase());
            pneus = pneuVulcanizadoRepository.findByUsuarioIdAndStatus(usuarioId, statusEnum, pageable);
        } else if (usuarioId != null) {
            pneus = pneuVulcanizadoRepository.findByUsuarioId(usuarioId, pageable);
        } else if (status != null) {
            StatusVulcanizacao statusEnum = StatusVulcanizacao.valueOf(status.toUpperCase());
            pneus = pneuVulcanizadoRepository.findByStatus(statusEnum, pageable);
        } else {
            pneus = pneuVulcanizadoRepository.findAllActive(pageable);
        }
        
        return pneus.map(this::converterParaResponseDTO);
    }

    /**
     * Exclui logicamente um pneu vulcanizado
     */
    public void excluir(Long id) {
        PneuVulcanizadoModel pneuVulcanizado = pneuVulcanizadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pneu vulcanizado não encontrado com ID: " + id));

        if (pneuVulcanizado.getDtDelete() != null) {
            throw new RuntimeException("Pneu vulcanizado já foi excluído");
        }

        pneuVulcanizado.setDtDelete(LocalDateTime.now());
        pneuVulcanizadoRepository.save(pneuVulcanizado);
    }

    /**
     * Deleta logicamente um pneu vulcanizado (alias para excluir)
     */
    public void deletar(Long id) {
        excluir(id);
    }

    /**
     * Lista pneus vulcanizados por status (aceita String)
     */
    @Transactional(readOnly = true)
    public List<PneuVulcanizadoResponseDTO> listarPorStatus(String status) {
        StatusVulcanizacao statusEnum = StatusVulcanizacao.valueOf(status.toUpperCase());
        return listarPorStatus(statusEnum);
    }

    /**
     * Conta pneus vulcanizados por usuário
     */
    @Transactional(readOnly = true)
    public long contarPorUsuario(Long usuarioId) {
        return pneuVulcanizadoRepository.countByUsuarioId(usuarioId);
    }

    /**
     * Conta pneus vulcanizados por status (aceita String)
     */
    @Transactional(readOnly = true)
    public long contarPorStatus(String status) {
        StatusVulcanizacao statusEnum = StatusVulcanizacao.valueOf(status.toUpperCase());
        return contarPorStatus(statusEnum);
    }

    /**
     * Conta pneus vulcanizados por status
     */
    @Transactional(readOnly = true)
    public Long contarPorStatus(StatusVulcanizacao status) {
        return pneuVulcanizadoRepository.countByStatus(status);
    }

    /**
     * Converte PneuVulcanizadoModel para PneuVulcanizadoResponseDTO
     */
    private PneuVulcanizadoResponseDTO converterParaResponseDTO(PneuVulcanizadoModel model) {
        String etiqueta = null;
        try {
            if (model.getProducao() != null && model.getProducao().getCarcaca() != null) {
                etiqueta = model.getProducao().getCarcaca().getNumero_etiqueta();
            }
        } catch (Exception ignored) {}

        return new PneuVulcanizadoResponseDTO(
                model.getId(),
                model.getUsuario().getId(),
                model.getUsuario().getNome(),
                model.getProducao().getId(),
                etiqueta,
                model.getStatus(),
                model.getDtCreate(),
                model.getDtUpdate()
        );
    }
}