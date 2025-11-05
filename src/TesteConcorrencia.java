/**
 * Teste automatizado para validar Trabalho 2a (Concorrência) e 2b (Memória Virtual)
 */
public class TesteConcorrencia {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TESTE: Trabalho 2a e 2b");
        System.out.println("Concorrência + Memória Virtual");
        System.out.println("========================================\n");
        
        // Teste 1: Memória Virtual com Page Faults
        System.out.println("\n=== TESTE 1: Memória Virtual com Page Faults ===");
        teste1MemoriaVirtual();
        
        // Teste 2: Múltiplos Processos Concorrentes
        System.out.println("\n\n=== TESTE 2: Múltiplos Processos Concorrentes ===");
        teste2MultiProcessos();
        
        // Teste 3: Estados de Processos
        System.out.println("\n\n=== TESTE 3: Estados de Processos ===");
        teste3Estados();
        
        System.out.println("\n========================================");
        System.out.println("TESTES CONCLUÍDOS");
        System.out.println("========================================");
    }
    
    private static void teste1MemoriaVirtual() {
        System.out.println("Criando sistema com memória pequena (64 palavras, página 8)");
        System.out.println("Isso força page faults durante execução\n");
        
        Sistema s = new Sistema(64, 8, 5);
        
        System.out.println("\n1. Criando processo fibonacci10");
        int pid1 = s.so.newProcess("fibonacci10");
        
        System.out.println("\n2. Verificando tabela de páginas (só primeira deve estar carregada)");
        System.out.println(s.so.dump(pid1));
        
        System.out.println("\n3. Verificando frames (deve ter 1 frame ocupado)");
        System.out.println(s.so.frames());
        
        System.out.println("\n4. Executando processo (deve causar page faults)");
        s.so.scheduler.setAutoSchedule(true);
        s.so.exec(pid1);
        
        System.out.println("\n5. Verificando frames após execução");
        System.out.println(s.so.frames());
        
        s.so.scheduler.shutdown();
        s.hw.cpu.stopCPU();
    }
    
    private static void teste2MultiProcessos() {
        System.out.println("Criando sistema normal (1024 palavras, página 8)");
        System.out.println("Testando execução concorrente de múltiplos processos\n");
        
        Sistema s = new Sistema(1024, 8, 3);
        
        System.out.println("\n1. Criando múltiplos processos");
        int pid1 = s.so.newProcess("progMinimo");
        int pid2 = s.so.newProcess("soma");
        int pid3 = s.so.newProcess("fatorial");
        
        System.out.println("\n2. Listando processos");
        System.out.println("Processos criados: " + pid1 + ", " + pid2 + ", " + pid3);
        
        System.out.println("\n3. Executando todos com escalonamento preemptivo");
        s.so.scheduler.setAutoSchedule(true);
        s.so.execAll();
        
        System.out.println("\n4. Processos terminaram");
        
        s.so.scheduler.shutdown();
        s.hw.cpu.stopCPU();
    }
    
    private static void teste3Estados() {
        System.out.println("Testando transições de estados de processos\n");
        
        Sistema s = new Sistema(512, 8, 5);
        
        System.out.println("\n1. Criando processo");
        int pid = s.so.newProcess("progMinimo");
        
        System.out.println("\n2. Verificando estado inicial (deve ser READY)");
        var pcb = s.so.getPCB(pid);
        System.out.println("Estado: " + pcb.state);
        
        System.out.println("\n3. Executando processo");
        s.so.scheduler.setAutoSchedule(true);
        s.so.exec(pid);
        
        System.out.println("\n4. Processo terminou (deve ser removido)");
        pcb = s.so.getPCB(pid);
        System.out.println("Processo ainda existe: " + (pcb != null));
        
        s.so.scheduler.shutdown();
        s.hw.cpu.stopCPU();
    }
}

