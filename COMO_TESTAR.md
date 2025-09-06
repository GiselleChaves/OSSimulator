# 🚀 COMO TESTAR O SO

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
so> help              # ver comandos e programas disponíveis
so> frames            # ver mapa de frames (GM)
so> new fatorial      # criar um processo (exemplos: fatorial, fibonacci10, PC)
so> new PC            # bubble sort
so> ps                # listar processos
so> dump 1            # ver PCB + mapeamento
so> execAll           # executar todos por RR
so> exit              # sair
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
