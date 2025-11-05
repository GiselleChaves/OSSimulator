# 🚀 Quick Start - SO Didático

## ⚡ Início Rápido (30 segundos)

### 1. Compilar
```bash
./compile.sh
```

### 2. Testar Memória Virtual
```bash
java -cp bin TesteMemoriaVirtual
```

### 3. Shell Interativo
```bash
java -cp bin Main
```

---

## 🎯 O Que Foi Implementado?

### ✅ Trabalho 2a - Sistema Concorrente
- **5 Threads** rodando simultaneamente
- **IO Assíncrono**: processos bloqueiam, outros executam
- **Shell Reativo**: aceita comandos durante execução

### ✅ Trabalho 2b - Memória Virtual
- **Lazy Loading**: só primeira página carregada
- **Page Fault**: detecção e tratamento automáticos
- **Disco de Paginação**: páginas vitimadas salvas em disco
- **Thread DiskDevice**: operações assíncronas de disco

---

## 📸 Ver Funcionando

### Exemplo 1: Lazy Loading
```bash
java -cp bin Main

so> new fibonacci10
so> dump 1
```

**Resultado:**
```
Tabela de páginas:
  pg0: frame=0, valid=true      ✅ Carregada
  pg1: not_loaded               ⏳ Não carregada
  pg2: not_loaded               ⏳ Não carregada
  pg3: not_loaded               ⏳ Não carregada
```

### Exemplo 2: Page Fault
```bash
so> exec 1
```

**Você verá nos logs:**
```
[PAGE_FAULT] Processo 1 acessou página 1 não carregada
[PAGE_FAULT] Bloqueando processo 1 até carga completar
[DISK] Processando LOAD_PAGE...
[INT_DISK] Processo 1 desbloqueado após carga de página
```

### Exemplo 3: Vitimação
```bash
java -cp bin Main --mem 64 --page 8

so> new fibonacci10
so> new fatorial
so> new PC
so> execAll
```

**Você verá nos logs:**
```
[EVICT] Vitimando página 1 do processo 1
[DISK] Salvando página 1 no disco com endereço 0
[EVICT] Frame liberado, página salva no disco
```

---

## 🔍 Comandos Úteis

### Ver Processos
```
so> ps
```

### Ver Memória de Processo
```
so> dump 1
```

### Ver Frames
```
so> frames
```

### Executar Todos
```
so> execAll
```

---

## 📊 Logs Importantes

| Prefixo | Significa |
|---------|-----------|
| `[PAGE_FAULT]` | Página não está em memória |
| `[DISK]` | Operação de disco |
| `[INT_DISK]` | Interrupção de disco |
| `[EVICT]` | Página sendo vitimada |
| `[CTX]` | Troca de contexto |
| `[IO]` | IO de console |

---

## 🎯 Testes Rápidos

### Teste 1: Ver Lazy Loading
```bash
java -cp bin Main
so> new fibonacci10
so> dump 1         # Só pg0 carregada
so> exit
```

### Teste 2: Ver Page Faults
```bash
java -cp bin Main
so> new fibonacci10
so> exec 1         # Observe [PAGE_FAULT] nos logs
so> exit
```

### Teste 3: Ver Vitimação
```bash
java -cp bin Main --mem 64 --page 8
so> new fibonacci10
so> new fatorial
so> new PC
so> execAll        # Observe [EVICT] nos logs
so> exit
```

---

## 📚 Documentação Completa

- **README.md** - Visão geral e comandos
- **IMPLEMENTACAO_COMPLETA_2B.md** - Detalhes técnicos
- **GUIA_DE_TESTES.md** - Testes passo a passo
- **STATUS.md** - Checklist de conformidade

---

## ✅ Validação Rápida

Execute estes 3 comandos para validar tudo:

```bash
# 1. Compilar
./compile.sh

# 2. Teste automatizado
java -cp bin TesteMemoriaVirtual

# 3. Teste manual
java -cp bin Main --mem 64 --page 8
# Dentro do shell: new fibonacci10, exec 1, dump 1, exit
```

Se todos funcionarem: **✅ IMPLEMENTAÇÃO COMPLETA**

---

## 🏆 O Que Esperar

### Durante Execução Normal
- Processos executando
- Troca de contexto (Round-Robin)
- Page faults ocasionais

### Com Memória Pequena (--mem 64)
- Page faults frequentes
- Vitimação de páginas
- Salvamento/carregamento do disco
- Bloqueio de processos

### Logs Típicos
```
GM: Primeira página (pg0) alocada
[CTX] Switch -> pid=1
[PAGE_FAULT] Processo 1 acessou página 1
[DISK] Carregando página 1
[INT_DISK] Processo 1 desbloqueado
[EVICT] Vitimando página 2
[DISK] Salvando página no disco
```

---

## 🆘 Problemas Comuns

### "Sem frames livres"
**Solução:** Memória muito pequena
```bash
java -cp bin Main --mem 512 --page 8
```

### Processos não executam
**Solução:** Usar execAll
```
so> execAll
```

### Não vejo logs de disco
**Solução:** Usar memória pequena
```bash
java -cp bin Main --mem 64 --page 8
```

---

## 🎓 Pronto!

Você agora tem um **Sistema Operacional Didático completo** com:
- ✅ Concorrência (5 threads)
- ✅ IO Assíncrono
- ✅ Memória Virtual
- ✅ Paginação com Disco

**Divirta-se explorando!** 🚀

