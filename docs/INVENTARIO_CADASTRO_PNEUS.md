# Inventário e política do cadastro de pneus

Issue: https://github.com/mycrosan/apigpfabrica/issues/2 — verificado em 05/09/2026.

## Caminhos encontrados

| Caminho | Consumidor/efeito | Tratamento |
|---|---|---|
| POST `/api/carcaca` | `appgp/lib/service/carcacaapi.dart`, tela adicionar | Validar DTO, referências e formatos no serviço antes de persistir |
| PUT `/api/carcaca/{id}` | Mesmo cliente, `editdatawidget.dart` | Repetir todas as validações de cadastro técnico |
| Spring Data REST `CarcacaRepository` em `/api/carcaca` | Exposição automática de escrita/associações | Desativar exportação; não oferecer bypass do serviço |
| POST `/api/carcaca/leitura-campo` | `leituracarcacaapi.dart`, foto por campo | Substituir chamada externa por OCR local; leitura não grava cadastro final |
| `ProducaoController` e `QualidadeController` | Persistem carcaça carregada para mudar status | Não recebem/substituem valores técnicos; preservar comportamento de status |
| `RegraController` e `CarcacaRejeitadaController` | Alteram regras/catálogos de combinações | Não são cadastro de carcaça; verificar compatibilidade em cada gravação técnica |
| Novas rotas v2 da spec | Aplicativo após migração | DTO → serviço → repositório; confirmações auditadas e transação |

Rotas citadas em documentos antigos de Expo/cadastro-técnico não estão implementadas neste repositório. Não usá-las como prova de cobertura.

## Política inicial configurável

`pneus.politica` em `application.yml`: timezone America/Sao_Paulo, ano DOT mínimo 2000, semana 01–53, ano futuro rejeitado. Bloqueio de semana futura fica desativado até validação da convenção ISO pela operação; tolerância configurável 0–2 semanas. Os quatro dígitos representam exclusivamente 2000–2099 neste fluxo. Legados fora da faixa exigem revisão, sem mudar o texto da evidência.

Famílias iniciais: métrica radial (205/55R16, 295/80R22.5), polegadas radial (7.50R16), flutuação (31X10.50R15LT). Normalização preserva dígitos, permitindo apenas caixa, espaços e separadores equivalentes. Formato não suportado exige revisão. Catálogo real ainda deve ser inventariado pela operação; não houve consulta ao banco produtivo.

Segmentos iniciais: passeio, van, caminhonete. Habilitar caminhões depende de catalogar exemplos e medir o OCR nesse segmento. Identificar país por texto explícito; não inferir pela marca. Combinação inédita não contorna dado ausente/inválido ou proibição.

## Responsáveis e decisões antes do piloto

| Decisão pendente | Responsável funcional | Condição de ativação |
|---|---|---|
| Confirmar famílias/segmentos e política de semana DOT | Responsável técnico da fábrica | Registrar decisão e ajustar configuração antes de bloqueio por semana |
| Nomear revisores/adjudicadores e agenda | Gestor da operação | Pelo menos revisão independente; não aprovar o próprio cadastro |
| Retenção e local das imagens | Gestor da operação e infraestrutura | Política definida antes de ativar coleta real |
| Hardware, volume diário e teto de custo | Infraestrutura e gestor | Dimensionar por teste local; não comprar recursos automaticamente |
| Metas e volume de fotos revisadas | Responsável pela qualidade | Congelar critérios e teste antes de promover sugestões |

Até essas decisões, pode-se implementar e testar com dados descartáveis. Nenhum percentual de precisão foi comprovado. Coleta, revisão e inferência não usam provedor externo.
