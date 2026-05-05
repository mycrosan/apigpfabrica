#!/bin/bash

# Script de Instalação das Regras de Codificação - GP Premium
# Este script configura automaticamente todas as regras e ferramentas

echo "🚀 Instalando Regras de Codificação - GP Premium"
echo "================================================"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para imprimir mensagens coloridas
print_step() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Verificar se estamos no diretório correto
if [ ! -f "pom.xml" ]; then
    print_error "Este script deve ser executado no diretório raiz do projeto (onde está o pom.xml)"
    exit 1
fi

print_step "Verificando ambiente..."

# Verificar Java
if ! command -v java &> /dev/null; then
    print_error "Java não encontrado. Instale Java 11+ antes de continuar."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
if [[ "$JAVA_VERSION" < "11" ]]; then
    print_error "Java 11+ é necessário. Versão atual: $JAVA_VERSION"
    exit 1
fi

print_success "Java $JAVA_VERSION encontrado"

# Verificar Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven não encontrado. Instale Maven 3.6+ antes de continuar."
    exit 1
fi

print_success "Maven encontrado"

# Verificar Git
if ! command -v git &> /dev/null; then
    print_warning "Git não encontrado. Algumas funcionalidades podem não funcionar."
else
    print_success "Git encontrado"
fi

print_step "Configurando Git Hooks..."

# Instalar pre-commit hook
if [ -d ".git" ]; then
    if [ -f "pre-commit-hook.sh" ]; then
        cp pre-commit-hook.sh .git/hooks/pre-commit
        chmod +x .git/hooks/pre-commit
        print_success "Pre-commit hook instalado"
    else
        print_warning "Arquivo pre-commit-hook.sh não encontrado"
    fi
else
    print_warning "Diretório .git não encontrado. Inicialize o repositório Git primeiro."
fi

print_step "Configurando .gitignore..."

# Backup do .gitignore atual se existir
if [ -f ".gitignore" ]; then
    cp .gitignore .gitignore.backup
    print_success "Backup do .gitignore criado (.gitignore.backup)"
fi

# Usar template atualizado se disponível
if [ -f ".gitignore.template" ]; then
    cp .gitignore.template .gitignore
    print_success ".gitignore atualizado com template"
fi

print_step "Testando configurações..."

# Testar compilação
echo "Testando compilação..."
if mvn clean compile -q; then
    print_success "Compilação bem-sucedida"
else
    print_error "Falha na compilação. Verifique os erros acima."
    exit 1
fi

# Testar Checkstyle
echo "Testando Checkstyle..."
if mvn checkstyle:check -q 2>/dev/null; then
    print_success "Checkstyle configurado corretamente"
else
    print_warning "Checkstyle encontrou problemas. Execute 'mvn checkstyle:check' para detalhes."
fi

# Testar SpotBugs
echo "Testando SpotBugs..."
if mvn spotbugs:spotbugs -q 2>/dev/null; then
    print_success "SpotBugs configurado corretamente"
else
    print_warning "SpotBugs encontrou problemas. Execute 'mvn spotbugs:gui' para detalhes."
fi

print_step "Criando atalhos úteis..."

# Criar script de validação rápida
cat > validate-code.sh << 'EOF'
#!/bin/bash
echo "🔍 Executando validação rápida..."
mvn clean compile && echo "✓ Compilação OK" || echo "✗ Erro na compilação"
mvn checkstyle:check -q && echo "✓ Checkstyle OK" || echo "⚠ Problemas no Checkstyle"
mvn test -q && echo "✓ Testes OK" || echo "✗ Testes falharam"
echo "Validação concluída!"
EOF

chmod +x validate-code.sh
print_success "Script validate-code.sh criado"

# Criar script de relatórios
cat > generate-reports.sh << 'EOF'
#!/bin/bash
echo "📊 Gerando relatórios de qualidade..."
mvn clean test jacoco:report spotbugs:spotbugs checkstyle:checkstyle
echo ""
echo "📁 Relatórios gerados:"
echo "  - Cobertura: target/site/jacoco/index.html"
echo "  - SpotBugs: target/spotbugs.html"
echo "  - Checkstyle: target/site/checkstyle.html"
echo ""
echo "🌐 Para visualizar, abra os arquivos HTML no navegador"
EOF

chmod +x generate-reports.sh
print_success "Script generate-reports.sh criado"

print_step "Configuração concluída!"

echo ""
echo "🎉 Instalação das regras de codificação concluída com sucesso!"
echo ""
echo "📚 Próximos passos:"
echo "  1. Leia o arquivo DEVELOPMENT.md para instruções detalhadas"
echo "  2. Configure sua IDE conforme CODING_STANDARDS.md"
echo "  3. Execute './validate-code.sh' para validação rápida"
echo "  4. Execute './generate-reports.sh' para relatórios detalhados"
echo ""
echo "🔧 Comandos úteis:"
echo "  - mvn clean compile          # Compilar projeto"
echo "  - mvn test                   # Executar testes"
echo "  - mvn checkstyle:check       # Verificar estilo"
echo "  - mvn spotbugs:gui           # Interface gráfica SpotBugs"
echo "  - mvn jacoco:report          # Relatório de cobertura"
echo ""
echo "📖 Documentação:"
echo "  - CODING_STANDARDS.md        # Padrões de codificação"
echo "  - DEVELOPMENT.md             # Guia do desenvolvedor"
echo "  - RULES_SUMMARY.md           # Resumo das regras"
echo ""
echo "✨ Bom desenvolvimento!"