/**
 * Demonstração interativa de Page Fault com Disco
 * Mostra em detalhes o funcionamento da memória virtual
 */
public class DemoPageFault {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║  DEMONSTRAÇÃO: PAGE FAULT COM DISCO                    ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        // Memória MUITO pequena para forçar page faults
        int tamMem = 64;    // 8 frames
        int tamPg = 8;
        int delta = 50;     // Fatia grande para não ter preempção
        
        System.out.println("Configuração:");
        System.out.println("  Memória: " + tamMem + " palavras (MUITO PEQUENA!)");
        System.out.println("  Frames: " + (tamMem/tamPg) + " frames disponíveis");
        System.out.println("  Página: " + tamPg + " palavras\n");
        
        try {
            Sistema s = new Sistema(tamMem, tamPg, delta);
            
            // ============================================================
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("PASSO 1: Criar processo (Lazy Loading)");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            int pid = s.so.newProcess("fibonacci10");
            Thread.sleep(500);
            
            System.out.println("\n✓ Processo criado. Verificando memória:");
            System.out.println(s.so.dump(pid));
            
            System.out.println("👉 OBSERVE: Apenas pg0 está carregada!");
            System.out.println("    As outras 3 páginas estão 'not_loaded'\n");
            
            // ============================================================
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("PASSO 2: Executar processo (gera Page Faults!)");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            System.out.println("Iniciando threads...");
            s.so.scheduler.setAutoSchedule(true);
            
            // Aguardar threads iniciarem
            Thread.sleep(1000);
            
            System.out.println("\n🔍 OBSERVE OS LOGS ACIMA:");
            System.out.println("   [PAGE_FAULT] - Página não está em memória");
            System.out.println("   [DISK] - Operações do dispositivo de disco");
            System.out.println("   [INT_DISK] - Interrupção quando disco termina");
            System.out.println("   [CTX] - Troca de contexto\n");
            
            Thread.sleep(2000);
            
            System.out.println("\n✓ Processo executado. Verificando estado:");
            System.out.println(s.so.dump(pid));
            
            // ============================================================
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("PASSO 3: Criar mais processos (força vitimação)");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            int pid2 = s.so.newProcess("PC");  // Programa grande (13 páginas)
            Thread.sleep(500);
            
            System.out.println("\n✓ Segundo processo criado");
            System.out.println("\nEstado da memória:");
            System.out.println(s.so.frames());
            
            System.out.println("\n👉 Com 8 frames e 2 processos, vai começar a faltar memória!");
            
            Thread.sleep(2000);
            
            // ============================================================
            System.out.println("\n═══════════════════════════════════════════════════════");
            System.out.println("PASSO 4: Verificar armazenamento em disco");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            int diskPages = s.so.getDiskDevice().getDiskStorageSize();
            System.out.println("📊 Páginas salvas no disco: " + diskPages);
            
            if (diskPages > 0) {
                System.out.println("✅ SUCESSO: Páginas foram vitimadas e salvas no disco!");
            } else {
                System.out.println("ℹ️  Ainda não houve vitimação (memória suficiente)");
            }
            
            // ============================================================
            System.out.println("\n═══════════════════════════════════════════════════════");
            System.out.println("RESUMO DA DEMONSTRAÇÃO");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            System.out.println("✅ Lazy Loading:");
            System.out.println("   - Apenas primeira página carregada ao criar processo");
            System.out.println("   - Demais páginas ficam 'not_loaded'\n");
            
            System.out.println("✅ Page Fault Assíncrono:");
            System.out.println("   - Detectado quando CPU acessa página inválida");
            System.out.println("   - Processo bloqueia (BLOCKED)");
            System.out.println("   - Thread Disco carrega página assincronamente");
            System.out.println("   - Processo desbloqueado quando disco termina\n");
            
            System.out.println("✅ Vitimação:");
            System.out.println("   - Quando memória cheia, páginas são vitimadas");
            System.out.println("   - Páginas salvas no disco via SAVE_PAGE");
            System.out.println("   - Podem ser recarregadas depois via LOAD_PAGE\n");
            
            System.out.println("✅ Threads Concorrentes:");
            System.out.println("   - Shell, CPU, Scheduler, IODevice, DiskDevice");
            System.out.println("   - CPU executa enquanto disco trabalha\n");
            
            // Finalizar
            s.so.scheduler.setAutoSchedule(false);
            s.so.scheduler.shutdown();
            s.hw.cpu.stopCPU();
            s.so.getIODevice().shutdown();
            s.so.getDiskDevice().shutdown();
            
            Thread.sleep(500);
            
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║  DEMONSTRAÇÃO CONCLUÍDA COM SUCESSO!                   ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

