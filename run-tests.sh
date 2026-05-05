#!/bin/bash

# Script para executar testes com saída detalhada e organizada
# Uso: ./run-tests.sh [nome-do-teste]

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Função para imprimir cabeçalho
print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}           🧪 EXECUTANDO TESTES GP PREMIUM${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
}

# Função para executar teste específico com detalhes
run_specific_test() {
    local test_class=$1
    echo -e "${CYAN}🔍 Testando classe: ${YELLOW}$test_class${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Executar o teste com output detalhado
    mvn test -Dtest=$test_class 2>&1 | while IFS= read -r line; do
        # Filtrar linhas relevantes
        if [[ $line =~ "Running br.compneusgppremium" ]]; then
            class_name=$(echo "$line" | sed 's/.*Running //' | sed 's/br\.compneusgppremium\.api\.//')
            echo -e "   ${PURPLE}📋 Executando classe:${NC} ${YELLOW}$class_name${NC}"
        elif [[ $line =~ "Tests run:" ]]; then
            if [[ $line =~ "Failures: 0" ]] && [[ $line =~ "Errors: 0" ]]; then
                echo -e "   ${GREEN}✅ Todos os métodos passaram${NC} - $line"
            else
                echo -e "   ${RED}❌ Alguns métodos falharam${NC} - $line"
            fi
        elif [[ $line =~ "FAILURE" ]] || [[ $line =~ "ERROR" ]]; then
            echo -e "   ${RED}🚨 Erro:${NC} $line"
        elif [[ $line =~ "BUILD SUCCESS" ]]; then
            echo -e "${GREEN}🎉 TESTE CONCLUÍDO COM SUCESSO!${NC}"
        elif [[ $line =~ "BUILD FAILURE" ]]; then
            echo -e "${RED}💥 TESTE FALHOU!${NC}"
        fi
    done
    
    echo ""
    echo -e "${BLUE}📊 Relatório completo em: target/surefire-reports/${NC}"
    echo ""
}

# Função para executar todos os testes com detalhes
run_all_tests() {
    echo -e "${YELLOW}🔍 Executando todos os testes com detalhes...${NC}"
    echo ""
    
    # Lista de classes de teste (baseada nas classes reais do projeto)
    local test_classes=(
        "UsuarioControllerTest"
        "AutenticacaoControllerTest" 
        "ConfiguracaoMaquinaControllerTest"
        "ProducaoControllerTest"
        "RegistroMaquinaControllerTest"
        "UsuarioRepositoryTest"
        "ProducaoRepositoryTest"
        "TokenServiceTest"
        "GpPremiumApplicationTests"
    )
    
    local passed=0
    local failed=0
    local total_methods=0
    local failed_methods=0
    
    for test_class in "${test_classes[@]}"; do
        echo -e "${CYAN}🧪 Testando classe: ${YELLOW}$test_class${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        
        # Executar teste e capturar resultado
        test_output=$(mvn test -Dtest=$test_class -q 2>&1)
        local exit_code=$?
        
        # Analisar resultado detalhado
        if echo "$test_output" | grep -q "Tests run:"; then
            test_summary=$(echo "$test_output" | grep "Tests run:" | tail -1)
            
            # Extrair números
            tests_run=$(echo "$test_summary" | sed 's/.*Tests run: \([0-9]*\).*/\1/')
            failures=$(echo "$test_summary" | sed 's/.*Failures: \([0-9]*\).*/\1/')
            errors=$(echo "$test_summary" | sed 's/.*Errors: \([0-9]*\).*/\1/')
            
            total_methods=$((total_methods + tests_run))
            failed_methods=$((failed_methods + failures + errors))
            
            if [ $exit_code -eq 0 ]; then
                echo -e "   ${GREEN}✅ Todos os $tests_run métodos passaram${NC}"
                ((passed++))
            else
                echo -e "   ${RED}❌ $failures falhas, $errors erros de $tests_run métodos${NC}"
                ((failed++))
                
                # Mostrar métodos que falharam
                if echo "$test_output" | grep -q "FAILURE\|ERROR"; then
                    echo -e "   ${YELLOW}📋 Métodos com problemas:${NC}"
                    echo "$test_output" | grep -E "(FAILURE|ERROR)" | head -3 | while read -r error_line; do
                        echo -e "      ${RED}• $error_line${NC}"
                    done
                fi
            fi
        else
            echo -e "   ${RED}❌ Erro na execução da classe${NC}"
            ((failed++))
        fi
        
        echo ""
    done
    
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}                  📊 RESUMO FINAL${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo -e "📋 ${CYAN}Classes testadas:${NC}"
    echo -e "   ✅ ${GREEN}Passaram: $passed classes${NC}"
    echo -e "   ❌ ${RED}Falharam: $failed classes${NC}"
    echo -e "   📊 ${YELLOW}Total: $((passed + failed)) classes${NC}"
    echo ""
    echo -e "🧪 ${CYAN}Métodos testados:${NC}"
    echo -e "   ✅ ${GREEN}Passaram: $((total_methods - failed_methods)) métodos${NC}"
    echo -e "   ❌ ${RED}Falharam: $failed_methods métodos${NC}"
    echo -e "   📊 ${YELLOW}Total: $total_methods métodos${NC}"
    echo ""
    
    if [ $failed -eq 0 ]; then
        echo -e "${GREEN}🎉 TODOS OS TESTES PASSARAM!${NC}"
    else
        echo -e "${YELLOW}⚠️  $failed classe(s) precisam de atenção${NC}"
    fi
    echo ""
}

# Função para executar teste rápido (modo silencioso)
run_quick_test() {
    local test_class=$1
    echo -e "${CYAN}⚡ Teste rápido: ${YELLOW}$test_class${NC}"
    
    mvn test -Dtest=$test_class -q > /tmp/test_output.log 2>&1
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "✅ ${GREEN}PASSOU${NC}"
    else
        echo -e "❌ ${RED}FALHOU${NC}"
        echo -e "${YELLOW}📋 Detalhes:${NC}"
        grep -E "(FAILURE|ERROR|Failed)" /tmp/test_output.log | head -3
    fi
    echo ""
}

# Função principal
main() {
    print_header
    
    case "${1:-}" in
        "quick")
            if [ -n "$2" ]; then
                run_quick_test "$2"
            else
                echo -e "${RED}❌ Uso: ./run-tests.sh quick [NomeDoTeste]${NC}"
            fi
            ;;
        "all")
            run_all_tests
            ;;
        "")
            run_all_tests
            ;;
        *)
            run_specific_test "$1"
            ;;
    esac
    
    echo -e "${BLUE}📁 Relatórios detalhados em: target/surefire-reports/${NC}"
    echo -e "${BLUE}📖 Manual completo em: MANUAL_TESTES.md${NC}"
    echo ""
    echo -e "${CYAN}💡 Dicas de uso:${NC}"
    echo -e "   ${YELLOW}./run-tests.sh${NC}                    - Executar todos os testes com detalhes"
    echo -e "   ${YELLOW}./run-tests.sh UsuarioControllerTest${NC} - Executar teste específico com detalhes"
    echo -e "   ${YELLOW}./run-tests.sh quick UsuarioControllerTest${NC} - Teste rápido (modo silencioso)"
}

# Executar função principal
main "$@"