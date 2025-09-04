# 🚀 COMO TESTAR O MINI-SO

## 1. Compilar (apenas um comando!)
```bash
./compile.sh
```

## 2. Testar o Sistema

### 🎮 **OPÇÃO 1: Shell Interativo (PRINCIPAL)**
```bash
java -cp bin Main
```
**Comandos para testar:**
```
so> new soma          # Criar processo
so> new loop          # Criar outro processo  
so> ps                # Listar processos
so> dump 1            # Ver paginação do processo 1
so> exec 1            # Executar processo 1 até STOP
so> ps                # Ver estado após execução
so> execAll           # Executar todos com Round-Robin
so> exit              # Sair
```

---

### 🧪 **OPÇÃO 2: Testes Automatizados**
```bash
java -cp bin TesteSistema
```

---

## 📋 **Funcionalidades Demonstradas**

Qualquer teste mostra:
- ✅ **GM Paginado**: tradução endereços, alocação frames
- ✅ **GP**: PCB, estados, comandos new/rm/ps/dump
- ✅ **Round-Robin**: preempção por Delta instruções
- ✅ **STOP**: finalização e desalocação automática
- ✅ **Multithread**: Shell + Escalonador + CPU
- ✅ **Logs detalhados**: criação, escalonamento, finalização

## 🎯 **Para Apresentação:**

**Use:** `java -cp bin Main`

Digite os comandos: `new soma`, `ps`, `dump 1`, `exec 1`, `exit`

Demonstra TODAS as funcionalidades obrigatórias de forma interativa! 