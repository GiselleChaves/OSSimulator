public class TesteSistema {
    
    public static void main(String[] args) {
        System.out.println("=== TESTES AUTOMATIZADOS DO SO ===\n");
        
        testePaginacaoBasica();
        testeRoundRobinComTresProcessos();
        testeStopDesalocaEscalona();
        testeFuncionamentoContinuo();
        
        System.out.println("=== TODOS OS TESTES CONCLUÍDOS ===");
    }
    
    public static void testePaginacaoBasica() {
        System.out.println("1. Teste Paginação Básica");
        System.out.println("   tamMem=1024, tamPg=8; new progMinimo (14 palavras) → numPages=2");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        int pid = sistema.so.newProcess("progMinimo");
        if (pid > 0) {
            String dump = sistema.so.dump(pid);
            System.out.println(dump);
            // Checagem simples: deve ter 2 páginas
            boolean ok = dump.contains("2 páginas") || dump.contains("2 páginas");
            System.out.println("   Assert numPages==2: " + (ok ? "OK" : "FALHOU"));
        }
        
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        System.out.println("   ✓ Teste de paginação básica concluído\n");
    }
    
    public static void testeRoundRobinComTresProcessos() {
        System.out.println("2. Teste Round-Robin com 3 processos");
        System.out.println("   Delta=5; new fatorial, new fibonacci10, new PC; execAll");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        int pid1 = sistema.so.newProcess("fatorial");
        int pid2 = sistema.so.newProcess("fibonacci10");
        int pid3 = sistema.so.newProcess("progMinimo");
        
        System.out.println("Processos criados: " + pid1 + ", " + pid2 + ", " + pid3);
        
        // Iniciar threads do sistema (Scheduler + CPU) para execAll funcionar
        Thread schedulerThread = new Thread(sistema.so.scheduler, "Scheduler");
        Thread cpuThread = new Thread(sistema.hw.cpu, "CPU");
        schedulerThread.setDaemon(true);
        cpuThread.setDaemon(true);
        schedulerThread.start();
        cpuThread.start();
        
        // Executar por um tempo limitado (deixar RR alternar)
        Thread execThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                sistema.so.scheduler.shutdown();
                sistema.hw.cpu.stopCPU();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        execThread.start();
        
        sistema.so.execAll();
        
        try {
            execThread.join();
            schedulerThread.join(500);
            cpuThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Teste Round-Robin concluído\n");
    }
    
    public static void testeStopDesalocaEscalona() {
        System.out.println("3. Teste STOP desaloca e escalona próximo");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        int pid1 = sistema.so.newProcess("fatorial"); // termina com STOP
        int pid2 = sistema.so.newProcess("progMinimo");
        
        // Execução dirigida: roda somente pid1 em modo debug
        sistema.so.exec(pid1);
        
        // Verificar se pid1 foi removido (dump deve acusar erro)
        String dump = sistema.so.dump(pid1);
        System.out.println(dump);
        boolean ok = dump.contains("não existe");
        System.out.println("   Assert pid1 removido: " + (ok ? "OK" : "FALHOU"));
        
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        System.out.println("   ✓ Teste STOP concluído\n");
    }
    
    public static void testeFuncionamentoContinuo() {
        System.out.println("4. Teste Funcionamento Contínuo");
        System.out.println("   Sem execAll, processos devem executar automaticamente (threads ativas)");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        // Iniciar threads do sistema
        Thread schedulerThread = new Thread(sistema.so.scheduler);
        Thread cpuThread = new Thread(sistema.hw.cpu);
        
        schedulerThread.setDaemon(true);
        cpuThread.setDaemon(true);
        
        schedulerThread.start();
        cpuThread.start();
        
        int pid1 = sistema.so.newProcess("fatorial");
        int pid2 = sistema.so.newProcess("progMinimo");
        
        // Aguardar um pouco para ver execução alternando
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        
        try {
            schedulerThread.join(500);
            cpuThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Teste funcionamento contínuo concluído\n");
    }
} 