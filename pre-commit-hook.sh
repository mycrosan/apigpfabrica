#!/bin/bash

# Pre-commit hook para GP Premium
# Este script executa verificações de qualidade antes de cada commit

echo "🔍 Executando verificações de qualidade..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para imprimir mensagens coloridas
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Verificar se Maven está disponível
if ! command -v mvn &> /dev/null; then
    print_error "Maven não encontrado. Instale o Maven para continuar."
    exit 1
fi

# Verificar se Java está disponível
if ! command -v java &> /dev/null; then
    print_error "Java não encontrado. Instale o Java 11+ para continuar."
    exit 1
fi

# Verificar versão do Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
if [[ "$JAVA_VERSION" < "11" ]]; then
    print_error "Java 11+ é necessário. Versão atual: $JAVA_VERSION"
    exit 1
fi

print_status "Ambiente validado (Java $JAVA_VERSION, Maven disponível)"

# 1. Compilação
echo ""
echo "📦 Verificando compilação..."
if mvn clean compile -q; then
    print_status "Compilação bem-sucedida"
else
    print_error "Falha na compilação. Corrija os erros antes de fazer commit."
    exit 1
fi

# 2. Testes unitários
echo ""
echo "🧪 Executando testes..."
if mvn test -q; then
    print_status "Todos os testes passaram"
else
    print_error "Alguns testes falharam. Corrija os testes antes de fazer commit."
    exit 1
fi

# 3. Checkstyle (se configurado)
echo ""
echo "📏 Verificando estilo de código..."
if mvn checkstyle:check -q 2>/dev/null; then
    print_status "Estilo de código aprovado"
else
    print_warning "Problemas de estilo encontrados. Execute 'mvn checkstyle:check' para detalhes."
    # Não bloqueia o commit, apenas avisa
fi

# 4. SpotBugs (se configurado)
echo ""
echo "🐛 Verificando bugs potenciais..."
if mvn spotbugs:check -q 2>/dev/null; then
    print_status "Nenhum bug potencial encontrado"
else
    print_warning "Bugs potenciais encontrados. Execute 'mvn spotbugs:check' para detalhes."
    # Não bloqueia o commit, apenas avisa
fi

# 5. Verificar se há arquivos grandes
echo ""
echo "📁 Verificando tamanho dos arquivos..."
large_files=$(find . -type f -size +10M -not -path "./target/*" -not -path "./.git/*")
if [ -n "$large_files" ]; then
    print_warning "Arquivos grandes encontrados:"
    echo "$large_files"
    print_warning "Considere usar Git LFS para arquivos grandes."
fi

# 6. Verificar se há secrets/senhas
echo ""
echo "🔐 Verificando por secrets..."
secrets_patterns=(
    "password\s*=\s*['\"][^'\"]*['\"]"
    "secret\s*=\s*['\"][^'\"]*['\"]"
    "api_key\s*=\s*['\"][^'\"]*['\"]"
    "token\s*=\s*['\"][^'\"]*['\"]"
    "jdbc:mysql://.*:.*@"
)

for pattern in "${secrets_patterns[@]}"; do
    if git diff --cached --name-only | xargs grep -l -i -E "$pattern" 2>/dev/null; then
        print_error "Possível secret encontrado no código. Remova antes de fazer commit."
        exit 1
    fi
done

print_status "Nenhum secret detectado"

# 7. Verificar mensagem de commit
echo ""
echo "💬 Verificando formato da mensagem de commit..."
commit_msg_file="$1"
if [ -n "$commit_msg_file" ] && [ -f "$commit_msg_file" ]; then
    commit_msg=$(cat "$commit_msg_file")
    
    # Verificar se segue o padrão Conventional Commits
    if [[ ! $commit_msg =~ ^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .+ ]]; then
        print_warning "Mensagem de commit não segue o padrão Conventional Commits."
        print_warning "Formato esperado: tipo(escopo): descrição"
        print_warning "Exemplo: feat(usuario): adicionar endpoint de criação"
        # Não bloqueia o commit, apenas avisa
    else
        print_status "Formato da mensagem de commit aprovado"
    fi
fi

# 8. Verificar se há TODOs ou FIXMEs
echo ""
echo "📝 Verificando TODOs e FIXMEs..."
todos=$(git diff --cached --name-only | xargs grep -n -i -E "(TODO|FIXME|XXX|HACK)" 2>/dev/null || true)
if [ -n "$todos" ]; then
    print_warning "TODOs/FIXMEs encontrados:"
    echo "$todos"
    print_warning "Considere resolver antes do commit ou documentar adequadamente."
fi

echo ""
print_status "Todas as verificações concluídas! ✨"
print_status "Commit autorizado. 🚀"

exit 0