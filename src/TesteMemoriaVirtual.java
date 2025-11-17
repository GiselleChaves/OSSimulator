import java.util.function.Supplier;

/**
 * Testes focados em Memória Virtual e operações de disco.
 *
 * 1. Lazy loading: apenas a primeira página deve estar carregada após criar o processo.
 * 2. Page fault automático: CPU gera fault e o disco trata a solicitação.
 * 3. Vitimação: com memória pequena e múltiplos processos, páginas precisam ser salvas no disco.
 */
public class TesteMemoriaVirtual {

    private static final long TIMEOUT_MS = 8000;
    private static final long POLL_MS = 50;

    public static void main(String[] args) {
        System.out.println("=== TESTE DE MEMÓRIA VIRTUAL / DISCO ===\n");

        Sistema sistema = new Sistema(64, 8, 12); // poucos frames para forçar fault/vitimação
        startCoreThreads(sistema);

        // -----------------------------------------------------------------
        // Teste 1: Lazy loading ao criar processo grande
        // -----------------------------------------------------------------
        System.out.println("1. Lazy loading ao criar processo 'fibonacci10'");
        int pidFib = sistema.so.newProcess("fibonacci10");

        String dumpInicial = sistema.so.dump(pidFib);
        boolean somentePg0 = dumpInicial.contains("pg0") && dumpInicial.contains("not_loaded");
        System.out.println("   Apenas pg0 carregada? " + (somentePg0 ? "OK" : "FALHOU"));

        // -----------------------------------------------------------------
        // Teste 2: Page fault gera operações de disco
        // -----------------------------------------------------------------
        System.out.println("2. Page fault dispara carregamento de página");
        boolean houveFault = waitFor(() ->
                sistema.so.getDiskDevice().getQueueSize() > 0 ||
                sistema.so.getDiskDevice().getDiskStorageSize() > 0,
                TIMEOUT_MS);
        System.out.println("   Operação de disco registrada: " + (houveFault ? "OK" : "FALHOU"));

        // -----------------------------------------------------------------
        // Teste 3: Vitimação com múltiplos processos
        // -----------------------------------------------------------------
        System.out.println("3. Vitimação com múltiplos processos grandes");
        int pidPC = sistema.so.newProcess("PC");
        int pidFat = sistema.so.newProcess("fatorialV2");

        boolean vitimizou = waitFor(() -> sistema.so.getDiskDevice().getDiskStorageSize() >= 2, TIMEOUT_MS);
        int paginasDisco = sistema.so.getDiskDevice().getDiskStorageSize();
        System.out.println("   Páginas salvas no disco (>=2 esperado): " + paginasDisco +
                (vitimizou ? " (OK)" : " (PARCIAL)"));

        // Aguarda processos finalizarem para encerrar teste
        waitFor(() ->
                sistema.so.getPCB(pidFib) == null &&
                sistema.so.getPCB(pidPC) == null &&
                sistema.so.getPCB(pidFat) == null,
                TIMEOUT_MS * 2);

        shutdownSistema(sistema);
        System.out.println("\n=== TESTE CONCLUÍDO ===");
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static void startCoreThreads(Sistema sistema) {
        Thread schedulerThread = new Thread(sistema.so.scheduler, "Scheduler");
        Thread cpuThread = new Thread(sistema.hw.cpu, "CPU");
        Thread ioThread = new Thread(sistema.so.getIODevice(), "IODevice");
        Thread diskThread = new Thread(sistema.so.getDiskDevice(), "DiskDevice");

        schedulerThread.setDaemon(true);
        cpuThread.setDaemon(true);
        ioThread.setDaemon(true);
        diskThread.setDaemon(true);

        schedulerThread.start();
        cpuThread.start();
        ioThread.start();
        diskThread.start();
    }

    private static void shutdownSistema(Sistema sistema) {
        sistema.so.scheduler.shutdown();
        sistema.hw.cpu.stopCPU();
        sistema.so.getIODevice().shutdown();
        sistema.so.getDiskDevice().shutdown();

        sistema.so.scheduler.wakeUp();
        sistema.hw.cpu.wakeUp();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean waitFor(Supplier<Boolean> condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return true;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return condition.get();
    }
}
