package software;

import java.util.List;
import java.util.Scanner;

public class Shell implements Runnable {
    private SO so;
    private boolean active;
    private Scanner scanner;

    public Shell(SO so) {
        this.so = so;
        this.active = true;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        System.out.println("=== Mini-SO Didático - Shell Iniciado ===");
        System.out.println("Comandos disponíveis:");
        System.out.println("  new <nome>     - Criar novo processo");
        System.out.println("  rm <pid>       - Remover processo");
        System.out.println("  ps             - Listar processos");
        System.out.println("  dump <pid>     - Dump de processo");
        System.out.println("  dumpM <i> <f>  - Dump da memória física");
        //System.out.println("  frames         - Mostrar mapa de frames de memória");
        //System.out.println("  exec <pid>     - Executar processo (com preempção)");
        //System.out.println("  execAll        - Executar todos os processos");
        System.out.println("  in <pid> <val> - Responder a pedido de IN do processo");
        //System.out.println("  traceOn        - Ativar trace");
        //System.out.println("  traceOff       - Desativar trace");
        System.out.println("  help           - Mostrar esta ajuda");
        System.out.println("  exit           - Sair do sistema");
        System.out.println();
        System.out.println("📋 PROGRAMAS DISPONÍVEIS PARA 'new <nome>':");
        System.out.println("  • fatorial     - Calcula fatorial de um número");
        System.out.println("  • fatorialV2   - Versão otimizada do fatorial");
        System.out.println("  • fibonacci10  - Calcula sequência Fibonacci");
        System.out.println("  • fibonacci10v2- Versão alternativa Fibonacci");
        System.out.println("  • fibonacciREAD- Fibonacci com entrada do usuário");
        System.out.println("  • PC           - Bubble sort (ordenação)");
        System.out.println("  • PB           - Programa com condicionais");
        System.out.println("  • progMinimo   - Programa mínimo para teste");
        //System.out.println("  • soma         - Programa simples de soma");
        //System.out.println("  • loop         - Loop infinito para testes");
        System.out.println();
        System.out.println("💡 Exemplos de uso:");
        System.out.println("   so> new fatorial");
        System.out.println("   so> new fibonacciREAD");
        System.out.println("   so> ps");
        //System.out.println("   so> exec 1");
        //System.out.println("   so> execAll");
        System.out.println();

        while (active) {
            System.out.print("so> ");
            String line = scanner.nextLine().trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "new":
                        handleNew(parts);
                        break;
                    case "rm":
                        handleRm(parts);
                        break;
                    case "ps":
                        handlePs();
                        break;
                    case "dump":
                        handleDump(parts);
                        break;
                    case "dumpm":
                        handleDumpM(parts);
                        break;
                    case "frames":
                        handleFrames();
                        break;
                    case "exec":
                        handleExec(parts);
                        break;
                    case "execall":
                        handleExecAll();
                        break;
                    case "in":
                        handleIn(parts);
                        break;
                    case "traceon":
                        handleTraceOn();
                        break;
                    case "traceoff":
                        handleTraceOff();
                        break;
                    case "help":
                        handleHelp();
                        break;
                    case "exit":
                        handleExit();
                        break;
                    default:
                        System.out.println("Comando desconhecido: " + command);
                        break;
                }
            } catch (Exception e) {
                System.out.println("ERRO: " + e.getMessage());
            }
        }
        
        scanner.close();
    }

    private void handleNew(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Uso: new <nome_programa>");
            return;
        }
        
        String nomeProg = parts[1];
        int pid = so.newProcess(nomeProg);
        
        if (pid > 0) {
            System.out.println("Processo criado com PID " + pid);
        }
    }

    private void handleRm(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Uso: rm <pid>");
            return;
        }
        
        try {
            int pid = Integer.parseInt(parts[1]);
            boolean removed = so.rm(pid);
            
            if (!removed) {
                System.out.println("Falha ao remover processo " + pid);
            }
        } catch (NumberFormatException e) {
            System.out.println("PID deve ser um número");
        }
    }

    private void handlePs() {
        List<PCB> processes = so.ps();
        
        if (processes.isEmpty()) {
            System.out.println("Nenhum processo na tabela de processos");
            return;
        }
        
        System.out.println("=== LISTA DE PROCESSOS ===");
        System.out.printf("%-5s %-15s %-10s %-5s %-8s\n", "PID", "NOME", "ESTADO", "PC", "PÁGINAS");
        System.out.println("------------------------------------------------");
        
        for (PCB pcb : processes) {
            System.out.printf("%-5d %-15s %-10s %-5d %-8d\n", 
                            pcb.pid, pcb.nome, pcb.state, pcb.pc, pcb.numPages);
        }
    }

    private void handleDump(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Uso: dump <pid>");
            return;
        }
        
        try {
            int pid = Integer.parseInt(parts[1]);
            String dumpResult = so.dump(pid);
            System.out.println(dumpResult);
        } catch (NumberFormatException e) {
            System.out.println("PID deve ser um número");
        }
    }

    private void handleDumpM(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Uso: dumpM <inicio> <fim>");
            return;
        }
        
        try {
            int ini = Integer.parseInt(parts[1]);
            int fim = Integer.parseInt(parts[2]);
            String dumpResult = so.dumpM(ini, fim);
            System.out.println(dumpResult);
        } catch (NumberFormatException e) {
            System.out.println("Endereços devem ser números");
        }
    }

    private void handleFrames() {
        System.out.println(so.frames());
    }

    private void handleExec(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Uso: exec <pid>");
            return;
        }
        
        try {
            int pid = Integer.parseInt(parts[1]);
            so.exec(pid);
        } catch (NumberFormatException e) {
            System.out.println("PID deve ser um número");
        }
    }

    private void handleExecAll() {
        so.execAll();
    }

    private void handleTraceOn() {
        so.traceOn();
    }

    private void handleTraceOff() {
        so.traceOff();
    }

    private void handleHelp() {
        System.out.println();
        System.out.println("=== AJUDA - SO Didático ===");
        System.out.println("Comandos disponíveis:");
        System.out.println("  new <nome>     - Criar novo processo");
        System.out.println("  rm <pid>       - Remover processo");
        System.out.println("  ps             - Listar processos");
        System.out.println("  dump <pid>     - Dump de processo");
        System.out.println("  dumpM <i> <f>  - Dump da memória física");
        //System.out.println("  frames         - Mostrar mapa de frames de memória");
        //System.out.println("  exec <pid>     - Executar processo (com preempção)");
        //System.out.println("  execAll        - Executar todos os processos");
        System.out.println("  in <pid> <val> - Responder a pedido de IN do processo");
        //System.out.println("  traceOn        - Ativar trace");
        //System.out.println("  traceOff       - Desativar trace");
        System.out.println("  help           - Mostrar esta ajuda");
        System.out.println("  exit           - Sair do sistema");
        System.out.println();
        System.out.println("📋 PROGRAMAS DISPONÍVEIS PARA 'new <nome>':");
        System.out.println("  • fatorial     - Calcula fatorial de um número");
        System.out.println("  • fatorialV2   - Versão otimizada do fatorial");
        System.out.println("  • fibonacci10  - Calcula sequência Fibonacci");
        System.out.println("  • fibonacci10v2- Versão alternativa Fibonacci");
        System.out.println("  • fibonacciREAD- Fibonacci com entrada do usuário");
        System.out.println("  • PC           - Bubble sort (ordenação)");
        System.out.println("  • PB           - Programa com condicionais");
        System.out.println("  • progMinimo   - Programa mínimo para teste");
        System.out.println("  • soma         - Programa simples de soma");
        System.out.println("  • loop         - Loop infinito para testes");
        System.out.println();
        System.out.println("💡 Exemplos de uso:");
        System.out.println("   so> new fatorial");
        System.out.println("   so> new fibonacciREAD");
        System.out.println("   so> ps");
        //System.out.println("   so> exec 1");
        //System.out.println("   so> execAll");
        System.out.println();
    }

    private void handleIn(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Uso: in <pid> <valor>");
            return;
        }

        try {
            int pid = Integer.parseInt(parts[1]);
            int value = Integer.parseInt(parts[2]);
            boolean accepted = so.provideInput(pid, value);
            if (accepted) {
                System.out.println(String.format("[Shell] Valor %d enviado ao processo %d", value, pid));
            }
        } catch (NumberFormatException e) {
            System.out.println("PID e valor devem ser números inteiros");
        }
    }

    private void handleExit() {
        System.out.println("Finalizando sistema...");
        active = false;
        so.scheduler.shutdown();
        so.hw.cpu.stopCPU();
    }

    public void shutdown() {
        active = false;
    }
} 