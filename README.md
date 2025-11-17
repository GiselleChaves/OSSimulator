# SO Didático - Simulador de Sistema Operacional

Este projeto implementa um simulador de sistema operacional didático em Java, conforme especificação do Prof. Fernando Dotti (PUCRS). O sistema inclui Gerenciamento de Memória paginado com Memória Virtual, Gerenciamento de Processos com três estados, Escalonador Round-Robin preemptivo, IO Assíncrono, e funcionamento multithread completo.

## ✅ Trabalhos Implementados

### 📋 Trabalho 2a - Sistema Concorrente (COMPLETO)
- **Thread de IO (Console)**: Processa pedidos de IN/OUT assincronamente
- **Estados de Processo**: NEW, READY, RUNNING, BLOCKED, TERMINATED
- **IO Assíncrono**: Processo bloqueia ao fazer IO, outro executa enquanto isso
- **DMA (Direct Memory Access)**: Dispositivo acessa memória diretamente
- **Fila de Bloqueados**: Processos aguardando IO ficam em fila separada
- **Interrupção de IO**: Quando IO termina, processo volta para READY
- **Sistema Reativo**: Shell aceita comandos enquanto processos executam

### 💾 Trabalho 2b - Memória Virtual (COMPLETO - Versão Avançada)
- **Lazy Loading**: Apenas primeira página carregada ao criar processo
- **Page Fault Assíncrono**: Detecta acesso a página não carregada e bloqueia processo
- **Dispositivo de Disco**: Thread separada para operações de paginação (SAVE_PAGE/LOAD_PAGE)
- **Política de Vítimas**: FIFO quando memória cheia, com salvamento no disco
- **Tabela de Páginas Estendida**: Flags valid/invalid, modified, lastAccessTime, diskAddress
- **Carregamento Sob Demanda**: Páginas carregadas do programa original ou do disco
- **Bloqueio Durante Disco**: Processo fica BLOCKED durante operações de disco
- **Interrupções de Disco**: Processo desbloqueado quando disco termina operação
- **DMA para Disco**: Disco acessa memória diretamente para salvar/carregar páginas
- **Três Estados de Página**: Nunca carregada / Em memória / Salva no disco

## Características Implementadas

### Gerenciamento de Memória (GM) Paginado com Memória Virtual
- Tabela de páginas por processo com flags (valid, modified, lastAccessTime)
- Tradução de endereços lógicos → físicos obrigatória
- Lazy loading: apenas primeira página carregada inicialmente
- Page fault automático quando página não está em memória
- Política de vítimas (FIFO) quando frames esgotam
- Alocação/desalocação de frames individuais

### Gerenciamento de Processos (GP)
- PCB completo com contexto da CPU
- Estados: NEW, READY, RUNNING, BLOCKED, TERMINATED
- Comandos: new, rm, ps, dump, dumpM, exec, execAll

### Escalonador Round-Robin
- Preempção por tempo (Delta = número de instruções)
- Fila READY thread-safe
- Troca de contexto automática via TIMER
- Funcionamento contínuo em thread separada

### Arquitetura Multithread Completa
- **Thread Shell**: Loop eterno aceitando comandos do usuário
- **Thread CPU**: Loop eterno executando instruções de processos
- **Thread Scheduler**: Loop eterno escalonando processos (READY → RUNNING)
- **Thread IODevice**: Loop eterno processando pedidos de IO Console (assíncrono)
- **Thread DiskDevice**: Loop eterno processando operações de paginação (assíncrono)

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
java -cp bin TesteSistema           # Testes básicos do sistema
java -cp bin TesteMemoriaVirtual    # Teste completo de Memória Virtual com Disco
```

**🔬 Teste de Memória Virtual com Page Faults:**
```bash
# Memória pequena força page faults
java -cp bin Main --mem 64 --page 8

# No shell:
so> new fibonacci10
so> dump 1           # Ver que só primeira página está carregada
so> exec 1           # Observar [PAGE_FAULT] nos logs
so> dump 1           # Ver páginas carregadas sob demanda
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

## 📊 Logs e Mensagens do Sistema

Durante a execução, o sistema exibe logs para facilitar o entendimento:

- **`[CTX]`** - Troca de contexto (escalonamento de processos)
- **`[SCHEDULER]`** - Operações do escalonador (bloquear, desbloquear)
- **`[IO]`** - Operações do dispositivo de IO (leitura/escrita)
- **`[SYSCALL]`** - Chamadas de sistema (IN, OUT, STOP)
- **`[INT_IO]`** - Interrupção de IO Console (dispositivo terminou operação)
- **`[INT_DISK]`** - Interrupção de Disco (operação de paginação concluída)
- **`[PAGE_FAULT]`** - Page fault (página não está em memória)
- **`[EVICT]`** - Página sendo vitimada (substituição)
- **`[DISK]`** - Operações do dispositivo de disco (save/load de páginas)
- **`[TIMER]`** - Preempção por tempo (fim da fatia)

### Exemplo de Log de Execução com Memória Virtual:
```
GM: Alocando primeira página para processo 1 (30 palavras, 4 páginas totais)
GM: Primeira página (pg0) alocada no frame 0
    Demais páginas serão carregadas sob demanda (page fault)
[SCHEDULER] Processo 1 adicionado à fila READY
[CTX] Switch -> pid=1 (fibonacci10) pc=0
                      pc: 5       exec: [5, LDI, 4, -1, 5]
[PAGE_FAULT] Processo 1 acessou página 1 não carregada
[PAGE_FAULT] Tratando page fault para processo 1, página 1
[PAGE_FAULT] Enviando pedido de carga de página ao disco...
[PAGE_FAULT] Bloqueando processo 1 até carga completar
[DISK] Operação de LOAD_PAGE adicionada à fila (processo 1, página 1)
[CTX] Switch -> pid=2 (fatorial) pc=0
[DISK] Processando LOAD_PAGE para processo 1, página 1...
[DISK] Carregando página 1 do programa 'fibonacci10' para frame 1
[DISK] LOAD_PAGE concluído: página 1 do processo 1 carregada no frame 1
[CPU] Interrupção de DISCO sinalizada para processo 1
[INT_DISK] Dispositivo de Disco terminou LOAD_PAGE para processo 1, página 1
[INT_DISK] Processo 1 desbloqueado após carga de página
[CTX] TIMER: preempção após 5 instruções | from pid=2 pc=8
[CTX] Switch -> pid=1 (fibonacci10) pc=12
```

### Exemplo de Log com Vitimação de Páginas:
```
[PAGE_FAULT] Tratando page fault para processo 2, página 2
[PAGE_FAULT] Sem frames livres, selecionando vítima...
[EVICT] Vitimando página 1 do processo 1 (frame 3)
[EVICT] Salvando página vitimada no disco...
[DISK] Salvando página 1 do processo 1 (frame 3)
[DISK] Página salva no disco com endereço 0
[EVICT] Frame 3 liberado, página salva no disco (addr=0)
[PAGE_FAULT] Enviando pedido de carga de página ao disco...
[DISK] Operação de LOAD_PAGE adicionada à fila (processo 2, página 2)
[PAGE_FAULT] Bloqueando processo 2 até carga completar
```

## Comandos do Shell

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `new <nome>` | Criar novo processo | `new soma` |
| `rm <pid>` | Remover processo | `rm 1` |
| `ps` | Listar processos | `ps` |
| `dump <pid>` | Dump de processo específico | `dump 1` |
| `dumpM <i> <f>` | Dump da memória física | `dumpM 0 50` |
| `exec <pid>` | Executar processo (modo debug) | `exec 1` |
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

### TesteSistema.java
Executa cenários-chave da Fase 2a (concorrência e IO assíncrono):
- **Execução automática**: `soma` e `fatorialV2` finalizam sem `exec`, registrando OUT 30 e 120.
- **IO assíncrono**: `fibonacciREAD` bloqueia em IN, recebe entrada via `provideInput` e termina.
- **Page fault**: `fibonacci10` gera faults e o disco trata as requisições.
- **Vitimação**: múltiplos processos grandes com poucos frames fazem páginas irem para o disco.

Comando: `java -cp bin TesteSistema`  
Resultado esperado: cada teste imprime “✓ Teste X finalizado” e resume os valores capturados.

### TesteMemoriaVirtual.java
Cobre os requisitos da Fase 2b (memória virtual + disco):
- **Lazy loading**: apenas pg0 carregada ao criar `fibonacci10`.
- **Page fault**: confirma operações do disco durante a execução.
- **Vitimação**: `fibonacci10`, `PC` e `fatorialV2` salvam páginas no disco quando a RAM enche.

Comando: `java -cp bin TesteMemoriaVirtual`  
Resultado esperado: status “OK” para cada etapa e contador de páginas persistidas no disco.

## Arquitetura do Sistema

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    Shell     │  │  Escalonador │  │     CPU      │  │   IODevice   │
│   Thread     │  │    Thread    │  │   Thread     │  │   Thread     │
│              │  │              │  │              │  │              │
│ - Comandos   │  │ - Fila READY │  │ - Execução   │  │ - IN/OUT     │
│ - new/rm/ps  │◄►│ - Round-Robin│◄►│ - Timer      │  │ - Console    │
│ - dump/exec  │  │ - Contexto   │  │ - Tradução   │  │ - DMA        │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
       │                   │                │  ▲              │
       └───────────────────┼────────────────┼──┼──────────────┘
                           │                │  │
                           │                ▼  │ interrupções
                    ┌──────────────────────────┴────┐
                    │           SO                  │
                    │                               │
                    │ - GM Paginado                 │
                    │ - GP (Estados de Processo)    │
                    │ - Tradução de Endereços       │
                    │ - Page Fault Handler          │
                    └───────────────────────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │      DiskDevice Thread       │
                    │                              │
                    │ - SAVE_PAGE (vitimação)      │
                    │ - LOAD_PAGE (page fault)     │
                    │ - DMA para Disco             │
                    │ - Armazenamento de páginas   │
                    └──────────────────────────────┘
```

## Estrutura de Arquivos

```
src/
├── Main.java              # Ponto de entrada com parsing de argumentos
├── Sistema.java           # Coordenação das threads principais
├── TesteSistema.java      # Testes automatizados
├── hardware/
│   ├── CPU.java          # CPU com preempção e tradução
│   ├── DiskDevice.java   # Dispositivo de disco para paginação (NOVO!)
│   ├── Hw.java           # Hardware com parâmetros configuráveis
│   ├── IODevice.java     # Dispositivo de IO Console
│   ├── Interrupts.java   # Tipos de interrupção incluindo TIMER
│   ├── Memory.java       # Memória com suporte a paginação
│   ├── Opcode.java       # Códigos de operação
│   └── Word.java         # Palavra de memória
├── menagers/
│   ├── MemoryManager.java # Gerenciador de frames
│   ├── Program.java      # Representação de programa
│   └── Programs.java     # Biblioteca de programas
├── software/
│   ├── InterruptHandling.java # Tratamento de interrupções (IO + Disco)
│   ├── PageTableEntry.java    # Entrada da tabela de páginas
│   ├── PCB.java          # Process Control Block
│   ├── Scheduler.java    # Escalonador Round-Robin
│   ├── Shell.java        # Interface de comandos
│   ├── SO.java           # Sistema Operacional principal
│   └── SysCallHandling.java # Tratamento de syscalls
└── util/
    └── Utilities.java    # Utilitários de debug e dump
```

## Critérios de Aceitação Implementados

### Trabalho 2a - Sistema Concorrente ✅
✅ **Thread de IO (Console)**: Processa IN/OUT assincronamente em thread separada  
✅ **Estados de Processo**: NEW, READY, RUNNING, BLOCKED, TERMINATED  
✅ **IO Assíncrono**: Processo bloqueia ao fazer IO, CPU executa outros enquanto isso  
✅ **DMA (Console)**: Dispositivo de IO acessa memória diretamente  
✅ **Fila de Bloqueados**: Processos aguardando IO ficam em fila separada  
✅ **Interrupção de IO**: Quando IO termina, processo desbloqueado e volta para READY  
✅ **Sistema Reativo (Shell)**: Shell em thread separada, aceita comandos continuamente  
✅ **Thread CPU**: Loop eterno executando instruções, verifica interrupções  
✅ **Thread Scheduler**: Escalonamento automático em thread separada  

### Trabalho 2b - Memória Virtual ✅
✅ **Lazy Loading**: Apenas primeira página carregada ao criar processo  
✅ **Page Fault Automático**: Detecta acesso a página não carregada  
✅ **Bloqueio Durante Page Fault**: Processo fica BLOCKED até disco carregar página  
✅ **Dispositivo de Disco**: Thread separada para operações de paginação  
✅ **Salvamento de Páginas Vitimadas**: Páginas salvas no disco quando memória cheia  
✅ **Carregamento do Disco**: Páginas previamente vitimadas são recarregadas  
✅ **Três Estados de Página**: Nunca carregada / Em memória / Salva no disco  
✅ **Interrupções de Disco**: Processo desbloqueado quando operação de disco termina  
✅ **DMA para Disco**: Disco acessa memória diretamente para save/load  
✅ **PageTableEntry Estendida**: Flags valid, modified, lastAccessTime, diskAddress  
✅ **Política de Vítimas**: FIFO simples para seleção de páginas a vitimar  

### Outros Requisitos ✅
✅ GM paginado com aloca/desaloca, carga por página, e tradução obrigatória  
✅ GP com PCB, running, fila READY e comandos exigidos  
✅ CPU conta instruções e aciona TIMER; InterruptHandling salva/restaura; RR funcionando  
✅ STOP finaliza processo, desaloca GM/PCB e escalona outro  
✅ execAll e funcionamento contínuo com threads separadas  
✅ dump mostra mapeamento lógico↔físico e PCB completo  
✅ Logs claros de criação, troca de contexto, STOP, page faults, vitimação e disco  

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