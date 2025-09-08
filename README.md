# SO Didático - Simulador de Sistema Operacional

Este projeto implementa um simulador de sistema operacional didático em Java, conforme especificação do Prof. Fernando Dotti (PUCRS). O sistema inclui Gerenciamento de Memória paginado, Gerenciamento de Processos, Escalonador Round-Robin com preempção por tempo, e funcionamento multithread.

## Características Implementadas

### Gerenciamento de Memória (GM) Paginado
- Tabela de páginas por processo
- Tradução de endereços lógicos → físicos obrigatória
- Carga de programas por página
- Alocação/desalocação de frames

### Gerenciamento de Processos (GP)
- PCB completo com contexto da CPU
- Estados: NEW, READY, RUNNING, BLOCKED, TERMINATED
- Comandos: new, rm, ps, dump, dumpM, exec, execAll

### Escalonador Round-Robin
- Preempção por tempo (Delta = número de instruções)
- Fila READY thread-safe
- Troca de contexto automática via TIMER
- Funcionamento contínuo em thread separada

### Arquitetura Multithread
- Thread Shell/SO: comandos interativos
- Thread Escalonador: gerencia fila READY
- Thread CPU: execução de instruções

## Como Compilar e Executar

### Compilação e Execução

#### 1. Compilar (apenas um comando!)
```bash
./compile.sh
```

#### 2. Executar o Sistema

**🎮 Shell Interativo (principal):**
```bash
java -cp bin Main
```
Comandos para testar: `help`, `frames`, `new fatorial`, `new PC`, `ps`, `dump 1`, `execAll`, `exit`

**🧪 Testes Automatizados:**
```bash
java -cp bin TesteSistema
```

**⚙️ Com Parâmetros Customizados:**
```bash
java -cp bin Main --mem 2048 --page 16 --delta 8
```

**❓ Ajuda:**
```bash
java -cp bin Main --help
```

## Parâmetros de Configuração

- `--mem <tamanho>`: Tamanho da memória em palavras (default: 1024)
- `--page <tamanho>`: Tamanho da página em palavras (default: 8)  
- `--delta <valor>`: Fatia de tempo em número de instruções (default: 5)
- `--help`: Mostrar ajuda

## Comandos do Shell

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `new <nome>` | Criar novo processo | `new soma` |
| `rm <pid>` | Remover processo | `rm 1` |
| `ps` | Listar processos | `ps` |
| `dump <pid>` | Dump de processo específico | `dump 1` |
| `dumpM <i> <f>` | Dump da memória física | `dumpM 0 50` |
| `exec <pid>` | Executar processo (com preempção) | `exec 1` |
| `execAll` | Executar todos os processos | `execAll` |
| `traceOn` | Ativar trace global | `traceOn` |
| `traceOff` | Desativar trace global | `traceOff` |
| `exit` | Sair do sistema | `exit` |

## Programas Disponíveis

- `fatorial`: Calcula fatorial de um número
- `fatorialV2`: Versão melhorada do fatorial
- `progMinimo`: Programa mínimo para teste
- `fibonacci10`: Calcula sequência de Fibonacci
- `fibonacci10v2`: Versão alternativa do Fibonacci
- `fibonacciREAD`: Fibonacci com entrada
- `PB`: Programa com condicionais
- `PC`: Bubble sort
- `soma`: Programa simples de soma (para testes)
- `loop`: Loop simples (para testes de preempção)

## Exemplo de Sessão

```
so> new soma
Processo criado com PID 1

so> new loop  
Processo criado com PID 2

so> ps
=== LISTA DE PROCESSOS ===
PID   NOME            ESTADO     PC    PÁGINAS
------------------------------------------------
1     soma            READY      0     2
2     loop            READY      0     1

so> dump 1
=== DUMP PROCESSO 1 ===
PCB[pid=1, nome=soma, state=READY, pc=0]
Registradores: r0=0 r1=0 r2=0 r3=0 r4=0 r5=0 r6=0 r7=0 r8=0 r9=0
Memória: 11 palavras, 2 páginas
Tabela de páginas: pg0→frame0 pg1→frame1
Mapeamento memória:
  Página 0 (end.lóg 0-7) → Frame 0 (end.fís 0-7)
  Página 1 (end.lóg 8-10) → Frame 1 (end.fís 8-15)

so> execAll
Iniciando execução escalonada de todos os processos...

so> exit
Finalizando sistema...
```

## Testes Automatizados

O arquivo `TesteSistema.java` contém 4 testes principais:

1. **Paginação Básica**: Verifica alocação de páginas e tradução de endereços
2. **Round-Robin**: Testa alternância entre 3 processos com preempção
3. **STOP**: Verifica finalização e desalocação de processo
4. **Funcionamento Contínuo**: Testa execução automática sem comando explícito

## Arquitetura do Sistema

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    Shell    │    │ Escalonador │    │     CPU     │
│   Thread    │    │   Thread    │    │   Thread    │
│             │    │             │    │             │
│ - Comandos  │    │ - Fila READY│    │ - Execução  │
│ - new/rm/ps │◄──►│ - Round-Robin│◄──►│ - Timer     │
│ - dump/exec │    │ - Contexto  │    │ - Tradução  │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌─────────────┐
                    │     SO      │
                    │             │
                    │ - GM Paginado│
                    │ - GP         │
                    │ - Tradução   │
                    └─────────────┘
```

## Estrutura de Arquivos

```
src/
├── Main.java              # Ponto de entrada com parsing de argumentos
├── Sistema.java           # Coordenação das threads principais
├── TesteSistema.java      # Testes automatizados
├── hardware/
│   ├── CPU.java          # CPU com preempção e tradução
│   ├── Hw.java           # Hardware com parâmetros configuráveis
│   ├── Interrupts.java   # Tipos de interrupção incluindo TIMER
│   ├── Memory.java       # Memória com suporte a paginação
│   ├── Opcode.java       # Códigos de operação
│   └── Word.java         # Palavra de memória
├── menagers/
│   ├── MemoryManager.java # Gerenciador de frames
│   ├── Program.java      # Representação de programa
│   └── Programs.java     # Biblioteca de programas
├── software/
│   ├── InterruptHandling.java # Tratamento de interrupções
│   ├── PCB.java          # Process Control Block
│   ├── Scheduler.java    # Escalonador Round-Robin
│   ├── Shell.java        # Interface de comandos
│   ├── SO.java           # Sistema Operacional principal
│   └── SysCallHandling.java # Tratamento de syscalls
└── util/
    └── Utilities.java    # Utilitários de debug e dump
```

## Critérios de Aceitação Implementados

✅ GM paginado com aloca/desaloca, carga por página, e tradução obrigatória  
✅ GP com PCB, running, fila READY e comandos exigidos  
✅ CPU conta instruções e aciona TIMER; InterruptHandling salva/restaura; RR funcionando  
✅ STOP finaliza processo, desaloca GM/PCB e escalona outro  
✅ execAll e funcionamento contínuo com threads separadas  
✅ dump mostra mapeamento lógico↔físico e PCB completo  
✅ Logs claros de criação, troca de contexto, STOP e faults  

## Desenvolvedor

- Giselle Gonçalves Chaves
- Gustavo Caldeira de Mesquita
- Henrique de Menezes Pinto Ribeiro
- Kauã Rodrigues Souza

## Observações

- Sistema implementado conforme especificação do Prof. Fernando Dotti
- Todas as funcionalidades obrigatórias estão presentes
- Código preparado para demonstração e avaliação
- Compatível com programas existentes na biblioteca Programs.java 