# 📱 Múltiplas Configurações por Celular

## 📋 Visão Geral

Esta funcionalidade permite que um mesmo celular tenha múltiplas configurações de máquina ao longo do tempo, sendo que a configuração ativa é sempre a mais recente.

## 🎯 Caso de Uso

O usuário pega o celular que vai estar vinculado a uma máquina e registra a matriz para aquela máquina que vai usar no dia. A matriz é como se fosse a forma do pneu que vai ser feita naquela máquina. Em outro dia ou outro momento, o usuário pode registrar outra matriz para o mesmo celular mas para a mesma máquina. A máquina atual vai ser sempre o último registro daquele celular.

## 🔧 Implementação Técnica

### Mudanças no Banco de Dados

#### Migration V1.2.0
```sql
-- Remove constraint única do celular_id
ALTER TABLE maquina_configuracao DROP CONSTRAINT IF EXISTS uq_celular;

-- Adiciona índice para performance
CREATE INDEX idx_maquina_configuracao_celular_dt_create 
ON maquina_configuracao(celular_id, dt_create);

-- Atualiza comentário da tabela
COMMENT ON TABLE maquina_configuracao IS 
'Configurações de máquinas. Permite múltiplas configurações por celular, sendo a mais recente a ativa.';
```

### Novos Endpoints

#### 1. Buscar Configuração Ativa
```
GET /api/configuracao-maquina/celular/{celularId}/ativa
```
**Descrição**: Retorna a configuração mais recente (ativa) para um celular específico.

## 🗑️ Soft Delete e Ativação Automática

### Comportamento do Soft Delete

A funcionalidade utiliza soft delete para manter o histórico de configurações:

- Configurações deletadas têm o campo `dt_delete` preenchido
- Apenas configurações com `dt_delete` nulo são consideradas ativas
- O histórico preserva todas as configurações, incluindo as deletadas
- Permite auditoria completa das mudanças de configuração

### Ativação Automática

Quando uma configuração é deletada (soft delete), o sistema automaticamente:

1. **Realiza o soft delete** da configuração atual
2. **Busca a configuração anterior** mais recente para a mesma máquina
3. **Mantém a configuração anterior ativa** automaticamente
4. **Garante que sempre há uma configuração ativa** (se existir histórico)

#### Endpoint de Exclusão Atualizado
```
DELETE /api/configuracao-maquina/{id}
```
**Descrição**: Remove uma configuração (soft delete) e automaticamente ativa a configuração anterior mais recente para a mesma máquina.

**Comportamento**:
- Se existe configuração anterior: ela se torna automaticamente ativa
- Se não existe configuração anterior: máquina fica sem configuração ativa
- Sempre preserva o histórico completo

**Resposta de Sucesso (200)**:
```json
{
  "id": 1,
  "celularId": "CEL001",
  "descricao": "Configuração Matriz A",
  "atributos": "...",
  "matriz": {
    "id": 1,
    "nome": "Matriz A"
  },
  "maquina": {
    "id": 1,
    "nome": "Máquina 01"
  },
  "dtCreate": "2024-01-15T10:30:00",
  "dtUpdate": "2024-01-15T10:30:00"
}
```

**Resposta quando não encontrado (404)**:
```json
{
  "message": "Nenhuma configuração ativa encontrada para o celular: CEL001"
}
```

#### 2. Listar Histórico de Configurações
```
GET /api/configuracao-maquina/celular/{celularId}/historico
```
**Descrição**: Lista todas as configurações de um celular, ordenadas da mais recente para a mais antiga.

**Resposta de Sucesso (200)**:
```json
[
  {
    "id": 3,
    "celularId": "CEL001",
    "descricao": "Configuração Matriz C",
    "dtCreate": "2024-01-17T14:20:00"
  },
  {
    "id": 2,
    "celularId": "CEL001", 
    "descricao": "Configuração Matriz B",
    "dtCreate": "2024-01-16T09:15:00"
  },
  {
    "id": 1,
    "celularId": "CEL001",
    "descricao": "Configuração Matriz A", 
    "dtCreate": "2024-01-15T10:30:00"
  }
]
```

### Novos Métodos no Repository

#### ConfiguracaoMaquinaRepository

```java
// Busca a configuração ativa (mais recente) por celular
Optional<ConfiguracaoMaquinaModel> findActiveByCelularId(String celularId);

// Lista todas as configurações por celular ordenadas por data
List<ConfiguracaoMaquinaModel> findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(String celularId);
```

## 🧪 Testes Implementados

### Testes de Unidade

#### Testes de Múltiplas Configurações

1. **testBuscarConfiguracaoAtivaPorCelular_ComConfiguracaoExistente_DeveRetornarConfiguracaoMaisRecente**
   - Verifica se retorna a configuração mais recente quando existem múltiplas

2. **testBuscarConfiguracaoAtivaPorCelular_SemConfiguracao_DeveRetornarNotFound**
   - Verifica se retorna 404 quando não há configurações

3. **testListarHistoricoConfiguracoesPorCelular_ComMultiplasConfiguracoes_DeveRetornarListaOrdenada**
   - Verifica se o histórico é retornado ordenado corretamente

4. **testCriarMultiplasConfiguracoesMesmoCelular_DevePermitirCriacao**
   - Verifica se é possível criar múltiplas configurações para o mesmo celular

#### Testes de Soft Delete e Ativação Automática

5. **testDeletarConfiguracao_ComConfiguracaoExistente_DeveRealizarSoftDelete**
   - Verifica se o soft delete é realizado corretamente

6. **testDeletarConfiguracao_ComConfiguracaoInexistente_DeveRetornarNotFound**
   - Verifica se retorna 404 para configurações inexistentes

7. **testDeletarConfiguracao_ComConfiguracaoAnteriorExistente_DeveManterConfiguracaoAnteriorAtiva**
   - Verifica se a configuração anterior permanece ativa após soft delete

8. **testDeletarConfiguracao_SemConfiguracaoAnterior_DeveApenasRealizarSoftDelete**
   - Verifica comportamento quando não há configuração anterior

## 📊 Fluxo de Uso

1. **Primeira Configuração**: Usuário cria configuração inicial para celular
2. **Nova Configuração**: Usuário cria nova configuração para o mesmo celular
3. **Busca Ativa**: Sistema sempre retorna a configuração mais recente
4. **Histórico**: Usuário pode consultar todas as configurações anteriores
5. **Exclusão com Ativação Automática**: 
   - Usuário deleta configuração atual
   - Sistema realiza soft delete
   - Configuração anterior automaticamente se torna ativa
   - Histórico completo é preservado

## 🔍 Considerações Importantes

- A configuração ativa é sempre determinada pela data de criação (`dt_create`)
- Configurações deletadas (`dt_delete` não nulo) não são consideradas
- O índice `idx_maquina_configuracao_celular_dt_create` garante performance nas consultas
- A funcionalidade é retrocompatível com configurações existentes

## 📈 Benefícios

- ✅ Flexibilidade para trocar matrizes ao longo do tempo
- ✅ Histórico completo de configurações
- ✅ Performance otimizada com índices adequados
- ✅ Retrocompatibilidade garantida
- ✅ Testes abrangentes para garantir qualidade