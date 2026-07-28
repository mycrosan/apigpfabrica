package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.dto.LeituraLateralDTO;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.MarcaModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.ModeloModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.MarcaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import br.compneusgppremium.api.util.OperationSystem;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconhece o pneu (foto da lateral) com a API do Claude: em vez de ler
 * marca/modelo/medida campo a campo, identifica qual pneu do catálogo é
 * aquele e, quando há ambiguidade, devolve de 1 a 4 candidatos ranqueados
 * para o operador escolher. DOT e país são lidos à parte, como texto.
 *
 * O operador sempre confere/confirma — este serviço apenas sugere.
 */
@Service
public class LeituraCarcacaService {

    // Haiku 4.5 (mais econômico) não suporta thinking adaptativo — por isso não
    // configuramos .thinking() abaixo; ao trocar de modelo, reavaliar essa escolha.
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final int MAX_CANDIDATOS = 4;
    // Quantas fotos de cadastros anteriores buscar como referência visual quando a
    // 1ª leitura fica ambígua (limita custo/latência da 2ª chamada à IA).
    private static final int MAX_REFERENCIAS = 3;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MarcaRepository marcaRepository;
    @Autowired
    private ModeloRepository modeloRepository;
    @Autowired
    private MedidaRepository medidaRepository;
    @Autowired
    private PaisRepository paisRepository;
    @Autowired
    private CarcacaRepository carcacaRepository;

    private volatile AnthropicClient client;

    // Estrutura que a IA é obrigada a devolver (structured outputs).
    // IDs usam 0 como "não encontrado no catálogo" para manter o schema simples.
    @JsonClassDescription("Resultado do reconhecimento do pneu pela foto da lateral")
    public static class LeituraIA {
        @JsonPropertyDescription("De 1 a 4 candidatos de pneu (marca+modelo+medida), do mais provável para o menos provável")
        public List<CandidatoIA> candidatos;

        @JsonPropertyDescription("ID do país no catálogo, ou 0 se não houver correspondência")
        public int paisId;
        @JsonPropertyDescription("País de fabricação como lido (MADE IN ...), ou string vazia se ilegível")
        public String paisTexto;
        @JsonPropertyDescription("Confiança da leitura do país: ALTA, MEDIA ou BAIXA")
        public String confiancaPais;

        @JsonPropertyDescription("Somente os 4 últimos dígitos do DOT (semana + ano, ex.: 2323), ou string vazia se ilegível")
        public String dot;
        @JsonPropertyDescription("Código DOT completo como lido, ou string vazia se ilegível")
        public String dotCompleto;
        @JsonPropertyDescription("Confiança da leitura do DOT: ALTA, MEDIA ou BAIXA")
        public String confiancaDot;
    }

    @JsonClassDescription("Um candidato de identificação do pneu")
    public static class CandidatoIA {
        @JsonPropertyDescription("ID da marca no catálogo, ou 0 se não houver correspondência")
        public int marcaId;
        @JsonPropertyDescription("Nome da marca como lido/reconhecido, ou string vazia se não identificado")
        public String marcaTexto;

        @JsonPropertyDescription("ID do modelo no catálogo, ou 0 se não houver correspondência")
        public int modeloId;
        @JsonPropertyDescription("Nome comercial do modelo como lido/reconhecido, ou string vazia se não identificado")
        public String modeloTexto;

        @JsonPropertyDescription("ID da medida no catálogo, ou 0 se não houver correspondência")
        public int medidaId;
        @JsonPropertyDescription("Medida como lida na lateral (ex.: 205/55R16), ou string vazia se ilegível")
        public String medidaTexto;

        @JsonPropertyDescription("Confiança deste candidato: ALTA, MEDIA ou BAIXA")
        public String confianca;
    }

    public LeituraLateralDTO lerLateral(String fotoBase64) {
        if (fotoBase64 == null || fotoBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("foto_base64 é obrigatória");
        }

        ImagemDecodificada foto = decodificarFoto(fotoBase64);

        LeituraIA primeira = chamarIA(List.of(
                ContentBlockParam.ofImage(imagemParam(foto)),
                ContentBlockParam.ofText(TextBlockParam.builder()
                        .text("Reconheça o pneu desta foto e preencha todos os campos.")
                        .build())));

        LeituraLateralDTO dto = converter(primeira);

        // Ambíguo (mais de 1 candidato): tenta refinar comparando com fotos de
        // cadastros já confirmados para os candidatos mais prováveis.
        if (dto.getCandidatos().size() > 1) {
            List<ContentBlockParam> blocosComparacao = montarBlocosComparacao(foto, dto.getCandidatos());
            if (blocosComparacao != null) {
                LeituraIA refinada = chamarIA(blocosComparacao);
                dto = converter(refinada);
            }
        }

        return dto;
    }

    private static class ImagemDecodificada {
        byte[] bytes;
        String base64;
        Base64ImageSource.MediaType mediaType;
    }

    private ImagemDecodificada decodificarFoto(String fotoBase64) {
        String data = fotoBase64.trim();
        Base64ImageSource.MediaType mediaType = Base64ImageSource.MediaType.IMAGE_JPEG;
        if (data.startsWith("data:")) {
            if (data.startsWith("data:image/png")) {
                mediaType = Base64ImageSource.MediaType.IMAGE_PNG;
            } else if (data.startsWith("data:image/webp")) {
                mediaType = Base64ImageSource.MediaType.IMAGE_WEBP;
            }
            int comma = data.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("foto_base64 em formato data-URI inválido");
            }
            data = data.substring(comma + 1);
        }
        // A API rejeita base64 com quebras de linha
        data = data.replaceAll("\\s", "");

        ImagemDecodificada img = new ImagemDecodificada();
        img.base64 = data;
        img.mediaType = mediaType;
        img.bytes = Base64.getDecoder().decode(data);
        return img;
    }

    private ImageBlockParam imagemParam(ImagemDecodificada img) {
        return ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(img.mediaType)
                        .data(img.base64)
                        .build())
                .build();
    }

    private LeituraIA chamarIA(List<ContentBlockParam> blocosUsuario) {
        StructuredMessageCreateParams<LeituraIA> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(4096L)
                // Catálogos + histórico no system prompt com cache (prefixo estável entre leituras)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(montarInstrucoes())
                                .build(),
                        TextBlockParam.builder()
                                .text(montarCatalogos())
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()))
                .outputConfig(LeituraIA.class)
                .addUserMessageOfBlockParams(blocosUsuario)
                .build();

        StructuredMessage<LeituraIA> message = getClient().messages().create(params);

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("A IA não retornou resultado para a foto enviada"))
                .text();
    }

    // Monta a 2ª chamada (foto nova + fotos de cadastros anteriores confirmados dos
    // candidatos top, como referência visual). Retorna null se nenhuma referência
    // foi encontrada (ex.: modelos novos, sem histórico de fotos ainda).
    private List<ContentBlockParam> montarBlocosComparacao(ImagemDecodificada fotoNova,
                                                             List<LeituraLateralDTO.CandidatoPneu> candidatos) {
        List<ContentBlockParam> blocos = new ArrayList<>();
        blocos.add(ContentBlockParam.ofText(TextBlockParam.builder()
                .text("FOTO NOVA — o pneu que precisa ser identificado:")
                .build()));
        blocos.add(ContentBlockParam.ofImage(imagemParam(fotoNova)));

        int referenciasEncontradas = 0;
        for (LeituraLateralDTO.CandidatoPneu c : candidatos) {
            if (referenciasEncontradas >= MAX_REFERENCIAS) {
                break;
            }
            ImagemDecodificada referencia = buscarFotoReferencia(c.getModeloId(), c.getMedidaId());
            if (referencia == null) {
                continue;
            }
            blocos.add(ContentBlockParam.ofText(TextBlockParam.builder()
                    .text("REFERÊNCIA — foto de um cadastro já confirmado para o candidato "
                            + descricaoCandidato(c) + ":")
                    .build()));
            blocos.add(ContentBlockParam.ofImage(imagemParam(referencia)));
            referenciasEncontradas++;
        }

        if (referenciasEncontradas == 0) {
            return null;
        }

        blocos.add(ContentBlockParam.ofText(TextBlockParam.builder()
                .text("Compare visualmente a FOTO NOVA com cada REFERÊNCIA acima (desenho da lateral, "
                        + "logotipo, texto). Se uma referência bater claramente, devolva só esse candidato "
                        + "com confiança ALTA. Se nenhuma bater ou a dúvida continuar, devolva os candidatos "
                        + "mais plausíveis de novo — inclusive algum que não tinha referência disponível. "
                        + "Releia também DOT e país a partir da FOTO NOVA.")
                .build()));
        return blocos;
    }

    private static String descricaoCandidato(LeituraLateralDTO.CandidatoPneu c) {
        String marca = c.getMarcaTexto() != null ? c.getMarcaTexto() : "";
        String modelo = c.getModeloTexto() != null ? c.getModeloTexto() : "";
        String medida = c.getMedidaTexto() != null ? c.getMedidaTexto() : "";
        return (marca + " " + modelo).trim() + " " + medida;
    }

    // Reaproveita a foto arquivada de um cadastro anterior com a mesma combinação
    // modelo+medida, já confirmado por um operador, como referência visual.
    private ImagemDecodificada buscarFotoReferencia(Integer modeloId, Integer medidaId) {
        if (modeloId == null || medidaId == null) {
            return null;
        }
        List<CarcacaModel> registros = carcacaRepository.findComFotoPorModeloMedida(modeloId, medidaId);
        String caminhoBase = new OperationSystem().placeImageSystem("carcaca");
        int verificados = 0;
        for (CarcacaModel registro : registros) {
            if (verificados >= 5) {
                break;
            }
            verificados++;
            try {
                List<String> nomes = JSON.readValue(registro.getFotos(), new TypeReference<List<String>>() {
                });
                if (nomes == null || nomes.isEmpty()) {
                    continue;
                }
                String nomeArquivo = nomes.get(0);
                File arquivo = new File(caminhoBase + nomeArquivo);
                if (!arquivo.exists()) {
                    continue;
                }
                ImagemDecodificada img = new ImagemDecodificada();
                img.bytes = Files.readAllBytes(arquivo.toPath());
                img.base64 = Base64.getEncoder().encodeToString(img.bytes);
                img.mediaType = mediaTypePorNomeArquivo(nomeArquivo);
                return img;
            } catch (Exception ex) {
                // foto ilegível/corrompida/ausente — tenta o próximo cadastro
            }
        }
        return null;
    }

    private static Base64ImageSource.MediaType mediaTypePorNomeArquivo(String nomeArquivo) {
        String nome = nomeArquivo.toLowerCase();
        if (nome.endsWith(".png")) {
            return Base64ImageSource.MediaType.IMAGE_PNG;
        }
        if (nome.endsWith(".webp")) {
            return Base64ImageSource.MediaType.IMAGE_WEBP;
        }
        return Base64ImageSource.MediaType.IMAGE_JPEG;
    }

    private LeituraLateralDTO converter(LeituraIA ia) {
        LeituraLateralDTO dto = new LeituraLateralDTO();

        List<LeituraLateralDTO.CandidatoPneu> candidatos = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        if (ia.candidatos != null) {
            for (CandidatoIA c : ia.candidatos) {
                if (candidatos.size() >= MAX_CANDIDATOS) {
                    break;
                }
                LeituraLateralDTO.CandidatoPneu candidato = new LeituraLateralDTO.CandidatoPneu();
                candidato.setMarcaId(validarId(c.marcaId, idsMarcas()));
                candidato.setMarcaTexto(vazioParaNull(c.marcaTexto));
                candidato.setModeloId(validarId(c.modeloId, idsModelos()));
                candidato.setModeloTexto(vazioParaNull(c.modeloTexto));
                candidato.setMedidaId(validarId(c.medidaId, idsMedidas()));
                candidato.setMedidaTexto(vazioParaNull(c.medidaTexto));
                candidato.setConfianca(normalizarConfianca(c.confianca, candidato.getModeloId() != null));

                // descarta candidato totalmente vazio e duplicatas (mesma combinação já sugerida)
                boolean vazio = candidato.getMarcaId() == null && candidato.getMarcaTexto() == null
                        && candidato.getModeloId() == null && candidato.getModeloTexto() == null
                        && candidato.getMedidaId() == null && candidato.getMedidaTexto() == null;
                String chave = candidato.getModeloId() + "|" + candidato.getMedidaId() + "|"
                        + candidato.getModeloTexto() + "|" + candidato.getMedidaTexto();
                if (!vazio && vistos.add(chave)) {
                    candidatos.add(candidato);
                }
            }
        }
        dto.setCandidatos(candidatos);

        dto.setPaisId(validarId(ia.paisId, idsPaises()));
        dto.setPaisTexto(vazioParaNull(ia.paisTexto));
        dto.setConfiancaPais(normalizarConfianca(ia.confiancaPais, dto.getPaisId() != null));

        String dot = vazioParaNull(ia.dot);
        if (dot != null && !dot.matches("\\d{4}")) {
            // garante o contrato: DOT sugerido sempre com 4 dígitos, senão vem vazio
            dot = null;
        }
        dto.setDot(dot);
        dto.setDotCompleto(vazioParaNull(ia.dotCompleto));
        dto.setConfiancaDot(normalizarConfianca(ia.confiancaDot, dto.getDot() != null));

        return dto;
    }

    private String montarInstrucoes() {
        return "Você reconhece pneus de carro, van e caminhonete (pneus de passeio/leves, não pneus de caminhão) "
                + "pela foto da lateral, para o cadastro de carcaças de uma recapadora.\n"
                + "Sua tarefa é IDENTIFICAR O PNEU como um todo — não ler campo a campo. Olhe o logotipo/nome da marca, "
                + "o nome comercial do modelo, o desenho e o texto da lateral, e a medida (ex.: 205/55R16), e decida "
                + "qual item do CATÁLOGO DE MODELOS (que já vem casado com marca) e qual MEDIDA correspondem a este pneu.\n"
                + "Devolva de 1 a " + MAX_CANDIDATOS + " candidatos em `candidatos`, ordenados do mais provável para o menos provável:\n"
                + "1. Se você tem certeza de qual é o pneu, devolva **apenas 1 candidato** com confiança ALTA.\n"
                + "2. Se houver ambiguidade real (ex.: dois modelos parecidos da mesma marca, texto parcialmente apagado que "
                + "pode ser mais de um item do catálogo), devolva de 2 a " + MAX_CANDIDATOS + " candidatos plausíveis para o "
                + "operador escolher — todos precisam ser itens que existem de fato no catálogo.\n"
                + "3. Se não conseguir identificar nada com confiança mínima, devolva 1 candidato com marcaId/modeloId/medidaId "
                + "= 0 e apenas o texto que conseguiu ler (é assim que o operador descobre marca/modelo novo a cadastrar).\n"
                + "4. Nunca invente um modeloId ou medidaId que não exista no catálogo fornecido; prefira 0 a chutar.\n"
                + "5. Confiança ALTA = identificação certa; MEDIA = plausível mas não certa; BAIXA = muito incerto.\n"
                + "6. Depois dos catálogos vem um HISTÓRICO DE CADASTROS já confirmados neste pátio (quantas vezes cada "
                + "modelo+medida já foi cadastrado). Use isso só como sinal de plausibilidade para desempatar candidatos "
                + "visualmente parecidos — nunca para ignorar o que você realmente vê na foto.\n"
                + "\n"
                + "Além de identificar o pneu, leia também estes dois campos independentes, sempre a partir do texto visível "
                + "(nunca por dedução):\n"
                + "- DOT: o código DOT completo e, separadamente, apenas os 4 dígitos finais "
                + "(semana + ano de fabricação, ex.: 2323 = 23ª semana de 2023).\n"
                + "- PAÍS: 'MADE IN ...' quando visível, casado com o catálogo de países.\n"
                + "Se DOT ou país estiverem ilegíveis, retorne 0 no ID, string vazia no texto e confiança BAIXA. "
                + "Nunca invente: prefira campo vazio a chute. O operador humano confere tudo.";
    }

    private String montarCatalogos() {
        StringBuilder sb = new StringBuilder();

        sb.append("CATÁLOGO DE MARCAS (id|descricao):\n");
        List<MarcaModel> marcas = new ArrayList<>();
        marcaRepository.findAll().forEach(marcas::add);
        marcas.sort(Comparator.comparing(m -> m.id));
        for (MarcaModel m : marcas) {
            sb.append(m.id).append("|").append(m.descricao).append("\n");
        }

        sb.append("\nCATÁLOGO DE MODELOS (id|descricao|marca):\n");
        List<ModeloModel> modelos = new ArrayList<>();
        modeloRepository.findAll().forEach(modelos::add);
        modelos.sort(Comparator.comparing(ModeloModel::getId));
        for (ModeloModel m : modelos) {
            sb.append(m.getId()).append("|").append(m.getDescricao()).append("|")
                    .append(m.getMarca() != null ? m.getMarca().descricao : "").append("\n");
        }

        sb.append("\nCATÁLOGO DE MEDIDAS (id|descricao):\n");
        List<MedidaModel> medidas = new ArrayList<>();
        medidaRepository.findAll().forEach(medidas::add);
        medidas.sort(Comparator.comparing(m -> m.id));
        for (MedidaModel m : medidas) {
            sb.append(m.id).append("|").append(m.descricao).append("\n");
        }

        sb.append("\nCATÁLOGO DE PAÍSES (id|descricao):\n");
        List<PaisModel> paises = new ArrayList<>();
        paisRepository.findAll().forEach(paises::add);
        paises.sort(Comparator.comparing(PaisModel::getId));
        for (PaisModel p : paises) {
            sb.append(p.getId()).append("|").append(p.getDescricao()).append("\n");
        }

        sb.append("\n").append(montarHistorico());

        return sb.toString();
    }

    // Frequência de cadastros já confirmados por combinação modelo+medida neste
    // pátio — sinal de plausibilidade pra ajudar a desempatar candidatos parecidos.
    private String montarHistorico() {
        List<Object[]> contagens = carcacaRepository.contarPorModeloMedida();
        if (contagens.isEmpty()) {
            return "HISTÓRICO DE CADASTROS JÁ CONFIRMADOS NESTE PÁTIO: nenhum ainda.\n";
        }
        contagens.sort((a, b) -> Long.compare((Long) b[2], (Long) a[2]));

        StringBuilder sb = new StringBuilder();
        sb.append("HISTÓRICO DE CADASTROS JÁ CONFIRMADOS NESTE PÁTIO (modeloId|medidaId|quantidade já cadastrada):\n");
        for (Object[] linha : contagens) {
            sb.append(linha[0]).append("|").append(linha[1]).append("|").append(linha[2]).append("\n");
        }
        return sb.toString();
    }

    private Set<Integer> idsMarcas() {
        Set<Integer> ids = new HashSet<>();
        marcaRepository.findAll().forEach(m -> ids.add(m.id));
        return ids;
    }

    private Set<Integer> idsModelos() {
        Set<Integer> ids = new HashSet<>();
        modeloRepository.findAll().forEach(m -> ids.add(m.getId()));
        return ids;
    }

    private Set<Integer> idsMedidas() {
        Set<Integer> ids = new HashSet<>();
        medidaRepository.findAll().forEach(m -> ids.add(m.id));
        return ids;
    }

    private Set<Integer> idsPaises() {
        Set<Integer> ids = new HashSet<>();
        paisRepository.findAll().forEach(p -> ids.add(p.getId()));
        return ids;
    }

    private static Integer validarId(int id, Set<Integer> idsValidos) {
        return (id > 0 && idsValidos.contains(id)) ? id : null;
    }

    private static String vazioParaNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static String normalizarConfianca(String confianca, boolean encontrado) {
        if (!encontrado) {
            return "BAIXA";
        }
        if (confianca == null) {
            return "MEDIA";
        }
        switch (confianca.trim().toUpperCase()) {
            case "ALTA":
                return "ALTA";
            case "MEDIA":
            case "MÉDIA":
                return "MEDIA";
            default:
                return "BAIXA";
        }
    }

    private AnthropicClient getClient() {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    String key = System.getenv("ANTHROPIC_API_KEY");
                    if (key == null || key.trim().isEmpty()) {
                        throw new IllegalStateException(
                                "Variável de ambiente ANTHROPIC_API_KEY não configurada no servidor");
                    }
                    c = AnthropicOkHttpClient.builder().apiKey(key).build();
                    client = c;
                }
            }
        }
        return c;
    }
}
