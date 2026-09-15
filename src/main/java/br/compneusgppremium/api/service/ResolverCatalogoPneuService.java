package br.compneusgppremium.api.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO.Candidato;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.ocr.OcrResposta;
import br.compneusgppremium.api.repository.MarcaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResolverCatalogoPneuService {
    private final MarcaRepository marcas;
    private final ModeloRepository modelos;
    private final MedidaRepository medidas;
    private final PaisRepository paises;
    private final ValidacaoFormatoPneuService formatos;
    private final SinonimosPaisService sinonimosPais;
    // Campos onde aproximar é aceitável: item de catálogo conferível na foto, e a lista de
    // aproximados nunca vira sugestão automática -- quem escolhe é o operador olhando a foto.
    // MEDIDA entrou aqui em 13/09/2026 por decisão explícita, revertendo a regra original da spec
    // ("nunca corrigir dígitos por aproximação", SPEC_IA_PNEUS_PRECISAO_E_BASE_REVISADA.md §4.3):
    // fotos reais mostraram o dígito inicial da medida cortado ou trocado ("175/70R14C" lido como
    // "75/70R14C" ou "975/70R14G") e sem tolerância nenhum candidato aparecia. O risco aceito é o
    // mesmo dos outros campos aproximados -- a lista pode trazer uma medida vizinha errada (ex.:
    // R15 por R14), e cabe ao operador conferir o flanco antes de confirmar. DOT segue de fora:
    // não há item de catálogo pra comparar, só dígitos soltos validados por calendário.
    private static final java.util.Set<String> CAMPOS_APROXIMACAO =
            java.util.Set.of("MARCA", "MODELO", "PAIS", "MEDIDA");
    private static final int DISTANCIA_MAXIMA_APROXIMACAO = 2;
    private static final double PROPORCAO_MAXIMA_APROXIMACAO = 0.25;
    private static final int MAXIMO_APROXIMADOS = 5;
    private static final int TAMANHO_MINIMO_APROXIMACAO = 4;
    private static final String MARCADOR_COMPACTO = "MADEIN";
    private static final int TAMANHO_MINIMO_MARCADOR = 4;
    private static final int TAMANHO_MAXIMO_MARCADOR = 8;
    private static final int SEM_CORRESPONDENCIA = Integer.MAX_VALUE;
    // Glifos que o OCR não consegue distinguir em relevo na borracha, medidos nas leituras reais:
    // "SUFER2DDG" e "SUPER2000" são o MESMO texto para o reconhecedor -- F/P têm a mesma haste com
    // laço, e 0/D/G/Q o mesmo corpo redondo. Tratar essas trocas como equivalentes não é escolher o
    // item mais parecido: é reconhecer que a imagem não carrega a informação que separa esses dois
    // desenhos. Conferido contra o catálogo real: colapsa 0 de 228 marcas e 4 de 1106 modelos.
    private static final Map<Character, Character> CLASSES_DE_GLIFO = Map.ofEntries(
            Map.entry('0', 'O'), Map.entry('D', 'O'), Map.entry('Q', 'O'), Map.entry('G', 'O'),
            Map.entry('F', 'P'),
            Map.entry('1', 'I'), Map.entry('L', 'I'),
            Map.entry('5', 'S'),
            Map.entry('8', 'B'),
            Map.entry('2', 'Z'));
    public List<Candidato> resolver(final String campo, final List<OcrResposta.Linha> linhas,
            final Integer marcaId, final Integer modeloId) {
        Map<Integer, String> catalogo = snapshot(campo, marcaId, modeloId);
        return resolverSnapshot(campo, linhas, catalogo, marcaId);
    }
    public Map<Integer, String> snapshot(final String campo, final Integer marcaId, final Integer modeloId) {
        if (marcaId != null && !marcas.existsById(marcaId)) {
            throw contexto();
        }
        if (modeloId != null) {
            var modelo = modelos.findById(modeloId).orElseThrow(this::contexto);
            if (marcaId != null && (modelo.getMarca() == null || !marcaId.equals(modelo.getMarca().getId()))) {
                throw contexto();
            }
        }
        Map<Integer, String> catalogo = new LinkedHashMap<>();
        switch (campo) {
            case "MARCA" -> marcas.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "MODELO" -> {
                if (marcaId == null) {
                    throw contexto();
                }
                modelos.findAll().forEach(item -> {
                    if (item.getMarca() != null && marcaId.equals(item.getMarca().getId())) {
                        catalogo.put(item.getId(), item.getDescricao());
                    }
                });
            }
            case "MEDIDA" -> medidas.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "PAIS" -> paises.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "DOT" -> { }
            default -> throw new CadastroPneuException("CAMPO_INVALIDO", "Campo de leitura inválido.");
        }
        return java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(catalogo));
    }
    public List<Candidato> resolverSnapshot(final String campo, final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo) {
        return resolverSnapshot(campo, linhas, catalogo, null);
    }

    /**
     * Cada campo é resolvido pela técnica que corresponde ao seu formato, e não por igualdade de texto:
     * medida é padrão numérico, país é vocabulário fechado, marca e modelo são texto restrito ao catálogo.
     * Em nenhum caminho um caractere ausente da imagem é completado pelo catálogo.
     */
    public List<Candidato> resolverSnapshot(final String campo, final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo, final Integer marcaId) {
        return switch (campo) {
            case "DOT" -> resolverDot(linhas);
            case "MEDIDA" -> resolverMedida(linhas, catalogo);
            case "PAIS" -> resolverPais(linhas, catalogo);
            default -> resolverTexto(campo, linhas, catalogo, prefixoDaMarca(campo, marcaId));
        };
    }

    // A medida é buscada DENTRO da linha, como o DOT: no flanco ela vem cercada de índice de carga e
    // sufixo de uso. Identidades iguais no catálogo (255/35 R18 e 255/35 ZR18) devolvem os dois itens,
    // porque a ambiguidade é do cadastro e quem decide é o operador.
    private List<Candidato> resolverMedida(final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo) {
        Map<String, List<Map.Entry<Integer, String>>> porIdentidade = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> item : catalogo.entrySet()) {
            for (String identidade : formatos.extrairMedidasCanonicas(item.getValue())) {
                porIdentidade.computeIfAbsent(identidade, chave -> new ArrayList<>()).add(item);
            }
        }
        Map<Integer, Candidato> candidatos = new LinkedHashMap<>();
        for (OcrResposta.Linha linha : linhas) {
            if (ignorar(linha)) {
                continue;
            }
            for (String identidade : formatos.extrairMedidasCanonicas(linha.texto())) {
                for (Map.Entry<Integer, String> item : porIdentidade.getOrDefault(identidade, List.of())) {
                    juntar(candidatos, item, linha.escore());
                }
            }
        }
        return List.copyOf(candidatos.values());
    }

    // Vocabulário fechado de países: o marcador "MADE IN" é removido quando existe, mas não é exigido —
    // em foto de perto ele costuma ficar fora do quadro. O que protege contra ruído é a correspondência
    // ter de bater com um dos itens cadastrados, não o prefixo.
    private List<Candidato> resolverPais(final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo) {
        Map<Integer, Candidato> candidatos = new LinkedHashMap<>();
        for (OcrResposta.Linha linha : linhas) {
            if (ignorar(linha)) {
                continue;
            }
            var alvos = sinonimosPais.alvos(linha.texto());
            if (alvos.isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, String> item : catalogo.entrySet()) {
                if (alvos.contains(sinonimosPais.normalizar(item.getValue()))) {
                    juntar(candidatos, item, linha.escore());
                }
            }
        }
        return List.copyOf(candidatos.values());
    }

    // Marca e modelo casam por sequência de TOKENS inteiros, nunca por substring: há 14 pares de marcas
    // em que um nome está contido no outro (LANDSAIL/ANDSAIL, SEMPERIT/SEMPERITE), e substring faria a
    // leitura de uma casar com a outra.
    private List<Candidato> resolverTexto(final String campo, final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo, final List<String> prefixoMarca) {
        boolean modelo = "MODELO".equals(campo);
        Map<Integer, Candidato> candidatos = new LinkedHashMap<>();
        for (OcrResposta.Linha linha : linhas) {
            if (ignorar(linha)) {
                continue;
            }
            List<String> lido = tokens(normalizar(campo, linha.texto()));
            if (lido.isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, String> item : catalogo.entrySet()) {
                List<String> cadastrado = tokens(normalizar(campo, item.getValue()));
                if (cadastrado.isEmpty()) {
                    // Descrição vazia no catálogo casaria com qualquer leitura; fica de fora.
                    continue;
                }
                if (corresponde(lido, cadastrado, prefixoMarca, modelo)) {
                    juntar(candidatos, item, linha.escore());
                }
            }
        }
        return List.copyOf(candidatos.values());
    }

    private boolean corresponde(final List<String> lido, final List<String> cadastrado,
            final List<String> prefixoMarca, final boolean modelo) {
        // Compara pela forma compacta (tokens colados, sem espaço) em vez de lista de tokens: o
        // catálogo cadastra "SUPER 2000" com espaço, mas o molde do pneu grava "SUPER2000" —
        // fronteira de token é convenção de quem digitou a descrição, não informação da imagem.
        // A ORDEM dos tokens continua obrigatória; só o espaço entre eles deixa de importar.
        if (compacto(lido).equals(compacto(cadastrado))) {
            return true;
        }
        if (!modelo) {
            return false;
        }
        // A descrição do modelo é cadastrada com a marca no início ("HIFLY HF-805 CHALLENGER DSRT"),
        // mas o pneu imprime só o modelo. Comparar também sem esse prefixo cobre 96% do catálogo.
        List<String> semMarca = semPrefixo(cadastrado, prefixoMarca);
        return compacto(semMarca).equals(compacto(lido)) || contemSequenciaCompacta(semMarca, lido);
    }

    private String compacto(final List<String> tokens) {
        return String.join("", tokens);
    }

    /**
     * Itens do catálogo próximos da leitura, para o operador ESCOLHER — nunca para o sistema decidir.
     *
     * <p>Existe porque o erro observado em foto real é de um ou dois caracteres num campo de
     * vocabulário conhecido: o país saiu {@code MADEANCHENA} onde o molde diz {@code MADE IN CHINA},
     * e sem tolerância nenhuma isso não vira nada na tela. A tolerância é deliberadamente curta
     * (no máximo dois caracteres, e no máximo um quarto do tamanho lido) e a lista sai ordenada pela
     * distância. MEDIDA e DOT ficam de fora por decisão da spec: dígito não se aproxima.
     *
     * <p>O chamador precisa manter esta lista separada dos candidatos exatos. Um item daqui nunca
     * pode virar sugestão automática — quem escolhe é o operador, olhando a foto. DOT fica de fora:
     * não há item de catálogo pra comparar, só dígitos validados por calendário.
     */
    public List<Candidato> resolverAproximado(final String campo, final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo, final Integer marcaId) {
        if (!CAMPOS_APROXIMACAO.contains(campo)) {
            return List.of();
        }
        List<String> prefixoMarca = prefixoDaMarca(campo, marcaId);
        Map<Integer, Candidato> melhores = new LinkedHashMap<>();
        Map<Integer, Integer> distancias = new LinkedHashMap<>();
        for (OcrResposta.Linha linha : linhas) {
            if (ignorar(linha)) {
                continue;
            }
            String lido = compacto(tokens(normalizar(campo, linha.texto())));
            if (lido.length() < TAMANHO_MINIMO_APROXIMACAO) {
                continue;
            }
            int tolerancia = tolerancia(lido);
            for (Map.Entry<Integer, String> item : catalogo.entrySet()) {
                int distancia = distanciaAteItem(campo, lido, item.getValue(), prefixoMarca, tolerancia);
                if (distancia > tolerancia) {
                    continue;
                }
                Integer anterior = distancias.get(item.getKey());
                if (anterior == null || distancia < anterior) {
                    distancias.put(item.getKey(), distancia);
                    melhores.put(item.getKey(), new Candidato(item.getKey(), item.getValue(), linha.escore()));
                }
            }
        }
        return melhores.values().stream()
                .sorted(Comparator.comparingInt((Candidato candidato) -> distancias.get(candidato.id()))
                        .thenComparing(Candidato::escore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAXIMO_APROXIMADOS)
                .toList();
    }

    // Cada campo tem a forma que o molde do pneu grava. O país costuma vir com o marcador colado
    // ao nome ("MADEINCHINA"), então a comparação considera as duas formas; o modelo é comparado
    // também sem o prefixo da marca, como no casamento exato.
    private int distanciaAteItem(final String campo, final String lido, final String descricao,
            final List<String> prefixoMarca, final int tolerancia) {
        List<String> cadastrado = tokens(normalizar(campo, descricao));
        if (cadastrado.isEmpty()) {
            return tolerancia + 1;
        }
        int menor = distancia(lido, compacto(cadastrado), tolerancia);
        if ("PAIS".equals(campo)) {
            menor = Math.min(menor, distancia(lido, MARCADOR_COMPACTO + compacto(cadastrado), tolerancia));
            menor = Math.min(menor, distanciaSeparandoMarcador(lido, compacto(cadastrado)));
        }
        if ("MODELO".equals(campo)) {
            List<String> semMarca = semPrefixo(cadastrado, prefixoMarca);
            menor = Math.min(menor, distancia(lido, compacto(semMarca), tolerancia));
        }
        return menor;
    }

    // Mesma letra desenhada de formas que o OCR confunde vira o mesmo caractere antes de comparar.
    private String porClasseDeGlifo(final String texto) {
        StringBuilder saida = new StringBuilder(texto.length());
        for (char caractere : texto.toCharArray()) {
            saida.append(CLASSES_DE_GLIFO.getOrDefault(caractere, caractere));
        }
        return saida.toString();
    }

    /**
     * O flanco grava sempre "MADE IN &lt;PAIS&gt;". Comparar a frase inteira soma os erros do marcador
     * aos erros do nome e estoura a tolerância; separar as duas partes isola o que importa.
     *
     * <p>Medido em foto real: o OCR devolveu {@code MADGANCHENA} — o marcador saiu com dois erros e
     * o país com um só. Junto dá três e não casa; separado, o país sozinho ({@code CHENA} contra
     * {@code CHINA}) casa com folga. O prefixo ainda precisa parecer o marcador, senão qualquer
     * texto que termine parecido com um país viraria candidato.
     */
    private int distanciaSeparandoMarcador(final String lido, final String nome) {
        if (lido.length() <= nome.length() + 2) {
            return SEM_CORRESPONDENCIA;
        }
        String prefixo = lido.substring(0, lido.length() - nome.length());
        if (prefixo.length() < TAMANHO_MINIMO_MARCADOR || prefixo.length() > TAMANHO_MAXIMO_MARCADOR
                || distancia(prefixo, MARCADOR_COMPACTO, DISTANCIA_MAXIMA_APROXIMACAO)
                        > DISTANCIA_MAXIMA_APROXIMACAO) {
            return SEM_CORRESPONDENCIA;
        }
        String sufixo = lido.substring(lido.length() - nome.length());
        // Nome já isolado do marcador aqui: nomes com mais de 4 letras (CHINA, KOREA, ITALY...)
        // usam a mesma tolerância máxima do resto do arquivo em vez da fórmula proporcional ao
        // tamanho, que arredondava pra 1 em nomes curtos e não pegava leitura real medida em foto
        // (13/09/2026: "MADEMI CHNA"/"WADEMI CHNA" -> nome "ICHNA" tem distância 2 até "CHINA").
        // Nomes de até 4 letras (USA, UA) continuam na fórmula proporcional: tolerar 2 erros ali
        // seria aceitar metade do nome trocada.
        int limite = nome.length() > TAMANHO_MINIMO_APROXIMACAO ? DISTANCIA_MAXIMA_APROXIMACAO : tolerancia(nome);
        int distanciaDoNome = distancia(sufixo, nome, limite);
        return distanciaDoNome <= limite ? distanciaDoNome : SEM_CORRESPONDENCIA;
    }

    private int tolerancia(final String lido) {
        return Math.min(DISTANCIA_MAXIMA_APROXIMACAO,
                Math.max(1, (int) Math.round(lido.length() * PROPORCAO_MAXIMA_APROXIMACAO)));
    }

    // Levenshtein com corte, sobre a forma por classe de glifo: passou da tolerância, não
    // interessa quanto passou.
    private int distancia(final String lidoOriginal, final String cadastradoOriginal, final int limite) {
        String lido = porClasseDeGlifo(lidoOriginal);
        String cadastrado = porClasseDeGlifo(cadastradoOriginal);
        if (Math.abs(lido.length() - cadastrado.length()) > limite) {
            return limite + 1;
        }
        int[] anterior = new int[cadastrado.length() + 1];
        int[] atual = new int[cadastrado.length() + 1];
        for (int coluna = 0; coluna <= cadastrado.length(); coluna++) {
            anterior[coluna] = coluna;
        }
        for (int linha = 1; linha <= lido.length(); linha++) {
            atual[0] = linha;
            int menorDaLinha = atual[0];
            for (int coluna = 1; coluna <= cadastrado.length(); coluna++) {
                int troca = lido.charAt(linha - 1) == cadastrado.charAt(coluna - 1) ? 0 : 1;
                atual[coluna] = Math.min(Math.min(anterior[coluna] + 1, atual[coluna - 1] + 1),
                        anterior[coluna - 1] + troca);
                menorDaLinha = Math.min(menorDaLinha, atual[coluna]);
            }
            if (menorDaLinha > limite) {
                return limite + 1;
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[cadastrado.length()];
    }

    // Sequência contígua de tokens do catálogo cuja forma compacta bate com a leitura: o flanco
    // costuma trazer só parte do nome do modelo. Contígua e na ordem lida — nunca substring livre
    // dentro de um token, e nunca tokens fora de ordem. Se mais de um modelo da marca contiver a
    // mesma sequência, todos viram candidatos e a decisão fica ambígua.
    private boolean contemSequenciaCompacta(final List<String> cadastrado, final List<String> lido) {
        String alvo = compacto(lido);
        if (alvo.isEmpty()) {
            return false;
        }
        for (int inicio = 0; inicio < cadastrado.size(); inicio++) {
            StringBuilder acumulado = new StringBuilder();
            for (int fim = inicio; fim < cadastrado.size(); fim++) {
                acumulado.append(cadastrado.get(fim));
                if (acumulado.length() > alvo.length()) {
                    break;
                }
                if (acumulado.toString().equals(alvo)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> semPrefixo(final List<String> cadastrado, final List<String> prefixo) {
        if (prefixo.isEmpty() || prefixo.size() >= cadastrado.size()
                || !cadastrado.subList(0, prefixo.size()).equals(prefixo)) {
            return cadastrado;
        }
        return cadastrado.subList(prefixo.size(), cadastrado.size());
    }

    private List<String> prefixoDaMarca(final String campo, final Integer marcaId) {
        if (!"MODELO".equals(campo) || marcaId == null) {
            return List.of();
        }
        return marcas.findById(marcaId).map(marca -> tokens(normalizar(campo, marca.getDescricao())))
                .orElseGet(List::of);
    }

    private List<String> tokens(final String texto) {
        if (texto.isBlank()) {
            return List.of();
        }
        return List.of(texto.split(" "));
    }

    private boolean ignorar(final OcrResposta.Linha linha) {
        return linha.texto() == null || (linha.escore() != null && !Double.isFinite(linha.escore()));
    }

    private void juntar(final Map<Integer, Candidato> candidatos, final Map.Entry<Integer, String> item,
            final Double escore) {
        candidatos.merge(item.getKey(), new Candidato(item.getKey(), item.getValue(), escore),
                (anterior, atual) -> Comparator.nullsFirst(Double::compareTo)
                        .compare(anterior.escore(), atual.escore()) >= 0 ? anterior : atual);
    }

    private List<Candidato> resolverDot(final List<OcrResposta.Linha> linhas) {
        Map<String, Candidato> candidatos = new LinkedHashMap<>();
        Pattern formato = Pattern.compile("(?<![A-Z0-9])([0-9]{4})(?![A-Z0-9])");
        for (OcrResposta.Linha linha : linhas) {
            if (ignorar(linha)) {
                continue;
            }
            var matcher = formato.matcher(linha.texto().toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                String dot = matcher.group(1);
                try {
                    formatos.validarDot(dot);
                    candidatos.putIfAbsent(dot, new Candidato(null, dot, linha.escore()));
                } catch (CadastroPneuException erro) {
                    // Candidato inválido é rejeitado; o texto original permanece na evidência da tentativa.
                    if (!erro.getCodigo().startsWith("DOT_")) {
                        throw erro;
                    }
                }
            }
        }
        return List.copyOf(candidatos.values());
    }

    private String normalizar(final String campo, final String valor) {
        if (valor == null) {
            return "";
        }
        if ("MEDIDA".equals(campo)) {
            return formatos.normalizarMedida(valor);
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private CadastroPneuException contexto() {
        return new CadastroPneuException("CONTEXTO_INVALIDO", "Confira a marca e o modelo antes da leitura.");
    }
}
