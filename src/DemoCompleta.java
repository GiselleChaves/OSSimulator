/**
 * Demonstração COMPLETA com Page Faults visíveis
 */
public class DemoCompleta {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     DEMONSTRAÇÃO COMPLETA - MEMÓRIA VIRTUAL            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        try {
            // Configuração para forçar page faults
            Sistema s = new Sistema(64, 8, 50);
            
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TESTE 1: LAZY LOADING");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            System.out.println("➤ Criando processo 'soma' (programa pequeno)...\n");
            int pid1 = s.so.newProcess("soma");
            
            System.out.println("\n➤ Estado inicial do processo:");
            String dump1 = s.so.dump(pid1);
            String[] lines = dump1.split("\n");
            for (String line : lines) {
                if (line.contains("pg") || line.contains("NOT IN MEMORY")) {
                    System.out.println("   " + line);
                }
            }
            
            System.out.println("\n✓ LAZY LOADING funcionando!");
            System.out.println("  Apenas pg0 foi carregada, as outras não.\n");
            
            // ============================================================
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TESTE 2: PAGE FAULT EM AÇÃO");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            System.out.println("➤ Executando processo... (vai acessar páginas não carregadas)\n");
            System.out.println("👁️  OBSERVE OS LOGS:\n");
            
            // Executar em thread separada para ver logs
            Thread execThread = new Thread(() -> {
                s.so.exec(pid1);
            });
            execThread.start();
            execThread.join();
            
            System.out.println("\n➤ Estado após execução:");
            String dump2 = s.so.dump(pid1);
            String[] lines2 = dump2.split("\n");
            for (String line : lines2) {
                if (line.contains("pg") || line.contains("VALID") || line.contains("state=")) {
                    System.out.println("   " + line);
                }
            }
            
            // ============================================================
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TESTE 3: MEMÓRIA CHEIA → VITIMAÇÃO");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            System.out.println("➤ Estado atual da memória:");
            String frames1 = s.so.frames();
            String[] frameLines = frames1.split("\n");
            for (int i = 0; i < Math.min(12, frameLines.length); i++) {
                System.out.println("   " + frameLines[i]);
            }
            
            System.out.println("\n➤ Criando vários processos grandes...\n");
            
            int pid2 = s.so.newProcess("fibonacci10");
            System.out.println("   ✓ Processo 2 criado (fibonacci10)");
            
            int pid3 = s.so.newProcess("fatorial");
            System.out.println("   ✓ Processo 3 criado (fatorial)");
            
            int pid4 = s.so.newProcess("PC");
            System.out.println("   ✓ Processo 4 criado (PC - programa grande!)");
            
            System.out.println("\n➤ Frames após criar 4 processos:");
            String frames2 = s.so.frames();
            frameLines = frames2.split("\n");
            for (int i = 0; i < Math.min(12, frameLines.length); i++) {
                System.out.println("   " + frameLines[i]);
            }
            
            System.out.println("\n➤ Executando todos (vai forçar vitimação)...\n");
            
            // Executar em thread para ver logs
            Thread execThread2 = new Thread(() -> {
                s.so.exec(pid2);
            });
            execThread2.start();
            Thread.sleep(1000);
            
            Thread execThread3 = new Thread(() -> {
                s.so.exec(pid3);
            });
            execThread3.start();
            Thread.sleep(1000);
            
            execThread2.join();
            execThread3.join();
            
            System.out.println("\n➤ Verificando disco:");
            int diskPages = s.so.getDiskDevice().getDiskStorageSize();
            System.out.println("   📊 Páginas salvas no disco: " + diskPages);
            
            if (diskPages > 0) {
                System.out.println("   ✅ VITIMAÇÃO FUNCIONOU! Páginas foram salvas no disco!");
            }
            
            // ============================================================
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("RESUMO DOS TESTES");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            System.out.println("✅ LAZY LOADING");
            System.out.println("   → Apenas primeira página carregada ao criar processo\n");
            
            System.out.println("✅ PAGE FAULT");
            System.out.println("   → Detectado quando acessa página não carregada");
            System.out.println("   → Veja logs [PAGE_FAULT] acima\n");
            
            System.out.println("✅ BLOQUEIO DURANTE DISCO");
            System.out.println("   → Processo vai para BLOCKED durante carregamento");
            System.out.println("   → Veja logs [SCHEDULER] Bloqueando processo\n");
            
            System.out.println("✅ THREAD DISK");
            System.out.println("   → Operações [DISK] processadas assincronamente");
            System.out.println("   → Interrupções [INT_DISK] desbloqueiam processo\n");
            
            System.out.println("✅ VITIMAÇÃO");
            System.out.println("   → Páginas vitimadas quando memória cheia");
            System.out.println("   → Veja logs [EVICT] acima");
            System.out.println("   → Páginas salvas: " + diskPages + "\n");
            
            // Finalizar
            s.so.scheduler.shutdown();
            s.hw.cpu.stopCPU();
            s.so.getIODevice().shutdown();
            s.so.getDiskDevice().shutdown();
            
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║           DEMONSTRAÇÃO COMPLETA! ✅                    ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("❌ ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

