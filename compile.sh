#!/bin/bash

echo "🔧 Compilando SO Didático..."

# Limpar compilação anterior
rm -rf bin

# Criar diretório bin se não existir
mkdir -p bin

# Compilar todos os arquivos Java
javac -d bin src/**/*.java src/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilação concluída com sucesso!"
    echo ""
    echo "📁 Arquivos compilados organizados em: ./bin/"
    echo ""
    echo "🚀 COMO TESTAR O PROGRAMA:"
    echo ""
    echo "1. SHELL INTERATIVO (principal):"
    echo "   java -cp bin Main"
    echo "   Comandos: new soma, ps, dump 1, exec 1, execAll, exit"
    echo ""
    echo "2. TESTES AUTOMATIZADOS:"
    echo "   java -cp bin TesteSistema"
    echo ""
    echo "3. DEMONSTRAÇÃO COMPLETA:"
    echo "   java -cp bin TesteSistemaDemo"
    echo ""
    echo "4. COM PARÂMETROS CUSTOMIZADOS:"
    echo "   java -cp bin Main --mem 2048 --page 16 --delta 8"
    echo ""
    echo "5. AJUDA:"
    echo "   java -cp bin Main --help"
    echo ""
    echo "🎯 Para testar rapidamente: java -cp bin Main"
    echo "🎬 Para ver a demo completa: java -cp bin TesteSistemaDemo"
else
    echo "❌ Erro na compilação!"
    exit 1
fi 