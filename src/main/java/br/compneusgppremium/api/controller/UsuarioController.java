package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.PerfilRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuario")
@Tag(name = "Usuários", description = "Operações relacionadas ao gerenciamento de usuários")
@SecurityRequirement(name = "Bearer Authentication")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PerfilRepository repositoryPerfil;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @GetMapping
    @Operation(
        summary = "Listar todos os usuários",
        description = "Retorna uma lista com todos os usuários cadastrados no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de usuários retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsuarioModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public List<UsuarioModel> findAll() {
        List<UsuarioModel> usuarios = new ArrayList<>();
        repository.findAll().forEach(usuarios::add);
        return usuarios;
    }

    @GetMapping("/me")
    @Operation(
        summary = "Obter dados do usuário logado",
        description = "Retorna os dados do usuário atualmente autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dados do usuário retornados com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsuarioModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        )
    })
    public ResponseEntity<UsuarioModel> obterUsuarioLogado() {
        try {
            Long usuarioId = usuarioLogadoUtil.getUsuarioIdLogado();
            Optional<UsuarioModel> usuarioOpt = repository.findById(usuarioId);
            return usuarioOpt
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar usuário por ID",
        description = "Retorna os dados de um usuário específico pelo seu ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsuarioModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<UsuarioModel> consultar(
        @Parameter(description = "ID do usuário", required = true, example = "1")
        @PathVariable("id") Long id
    ) {
        Optional<UsuarioModel> usuarioOpt = repository.findById(id);
        return usuarioOpt
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping
    @Transactional
    @Operation(
        summary = "Criar novo usuário",
        description = "Cria um novo usuário no sistema com validação de login único e criptografia de senha"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsuarioModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou login já existe"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> criarUsuario(
        @Parameter(description = "Dados do usuário a ser criado", required = true)
        @RequestBody UsuarioModel usuarioRequest
    ) {
        try {
            if (loginJaExiste(usuarioRequest.getLogin())) {
                return ResponseEntity.badRequest().body("Login já existe!");
            }

            criptografarSenha(usuarioRequest);

            List<PerfilModel> perfisValidados = carregarPerfisValidados(usuarioRequest.getPerfil());
            usuarioRequest.setPerfil(perfisValidados);

            UsuarioModel savedUser = repository.save(usuarioRequest);

            return ResponseEntity.status(201).body(savedUser);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao criar usuário: " + e.getMessage());
        }
    }
    @PutMapping("/{id}")
    @Transactional
    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente, incluindo validação de perfis"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário atualizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsuarioModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> atualizarUsuario(
        @Parameter(description = "ID do usuário", required = true, example = "1")
        @PathVariable("id") Long id,
        @Parameter(description = "Dados atualizados do usuário", required = true)
        @RequestBody UsuarioModel usuarioRequest
    ) {
        return repository.findById(id)
                .map(usuarioExistente -> {
                    // Atualiza os campos
                    usuarioExistente.setNome(usuarioRequest.getNome());
                    usuarioExistente.setLogin(usuarioRequest.getLogin());

                    // Se a senha foi alterada, criptografa a nova senha
                    if (usuarioRequest.getPassword() != null && !usuarioRequest.getPassword().isEmpty()) {
                        usuarioExistente.setPassword(encoder.encode(usuarioRequest.getPassword()));
                    }

//                    usuarioExistente.setStatus(usuarioRequest.getStatus());

                    // Valida e atualiza os perfis
                    List<PerfilModel> perfisValidados = carregarPerfisValidados(usuarioRequest.getPerfil());
                    usuarioExistente.setPerfil(perfisValidados);

                    UsuarioModel usuarioAtualizado = repository.save(usuarioExistente);
                    return ResponseEntity.ok(usuarioAtualizado);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir usuário",
        description = "Remove um usuário do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário excluído com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> delete(
        @Parameter(description = "ID do usuário", required = true, example = "1")
        @PathVariable("id") Long id
    ) {
        return repository.findById(id)
                .map(usuario -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }


    private boolean loginJaExiste(String login) {
        return repository.findByLogin(login).isPresent();
    }

    private void criptografarSenha(UsuarioModel usuario) {
        usuario.setPassword(encoder.encode(usuario.getPassword()));
    }

    private List<PerfilModel> carregarPerfisValidados(List<PerfilModel> perfisEnviados) {
        if (perfisEnviados == null || perfisEnviados.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> idsPerfis = extrairIdsDosPerfis(perfisEnviados);
        List<PerfilModel> perfisEncontrados = buscarPerfisPorIds(idsPerfis);

        if (perfisEncontrados.size() != idsPerfis.size()) {
            throw new RuntimeException("Um ou mais perfis informados não existem.");
        }

        return perfisEncontrados;
    }

    private List<Integer> extrairIdsDosPerfis(List<PerfilModel> perfis) {
        List<Integer> ids = new ArrayList<>();
        for (PerfilModel perfil : perfis) {
            ids.add(perfil.getId());
        }
        return ids;
    }

    private List<PerfilModel> buscarPerfisPorIds(List<Integer> idsPerfis) {
        return (List<PerfilModel>) repositoryPerfil.findAllById(idsPerfis);
    }


}
