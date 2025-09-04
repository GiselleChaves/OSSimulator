public class TesteSistema {
    
    public static void main(String[] args) {
        System.out.println("=== TESTES AUTOMATIZADOS DO MINI-SO ===\n");
        
        testePaginacaoBasica();
        testeRoundRobinComTresProcessos();
        testeStopDesalocaEscalona();
        testeFuncionamentoContinuo();
        
        System.out.println("=== TODOS OS TESTES CONCLUÍDOS ===");
    }
    
    public static void testePaginacaoBasica() {
        System.out.println("1. Teste Paginação Básica");
        System.out.println("   tamMem=1024, tamPg=8; new p1 com 40 palavras → numPages=5");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        // Simular criação de processo
        int pid = sistema.so.newProcess("soma"); // programa pequeno para teste
        if (pid > 0) {
            String dump = sistema.so.dump(pid);
            System.out.println(dump);
        }
        
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        System.out.println("   ✓ Teste de paginação básica concluído\n");
    }
    
    public static void testeRoundRobinComTresProcessos() {
        System.out.println("2. Teste Round-Robin com 3 processos");
        System.out.println("   Delta=5; new p1, new p2, new p3, execAll");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        // Criar 3 processos
        int pid1 = sistema.so.newProcess("soma");
        int pid2 = sistema.so.newProcess("loop");
        int pid3 = sistema.so.newProcess("progMinimo");
        
        System.out.println("Processos criados: " + pid1 + ", " + pid2 + ", " + pid3);
        
        // Executar por um tempo limitado
        Thread execThread = new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 segundos
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Teste Round-Robin concluído\n");
    }
    
    public static void testeStopDesalocaEscalona() {
        System.out.println("3. Teste STOP desaloca e escalona próximo");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        int pid1 = sistema.so.newProcess("soma"); // termina com STOP
        int pid2 = sistema.so.newProcess("loop");
        
        // Executar processo que termina
        sistema.so.exec(pid1);
        
        // Verificar se foi desalocado
        String dump = sistema.so.dump(pid1);
        System.out.println(dump);
        
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        System.out.println("   ✓ Teste STOP concluído\n");
    }
    
    public static void testeFuncionamentoContinuo() {
        System.out.println("4. Teste Funcionamento Contínuo");
        System.out.println("   Sem execAll, processos devem executar automaticamente");
        
        Sistema sistema = new Sistema(1024, 8, 5);
        
        // Iniciar threads do sistema
        Thread schedulerThread = new Thread(sistema.so.scheduler);
        Thread cpuThread = new Thread(sistema.hw.cpu);
        
        schedulerThread.setDaemon(true);
        cpuThread.setDaemon(true);
        
        schedulerThread.start();
        cpuThread.start();
        
        // Criar processos - devem executar automaticamente
        int pid1 = sistema.so.newProcess("soma");
        int pid2 = sistema.so.newProcess("progMinimo");
        
        // Aguardar um pouco para ver execução
        try {
            Thread.sleep(1000);
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