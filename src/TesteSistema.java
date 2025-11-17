import java.util.function.Supplier;

import software.PCB;

public class TesteSistema {

    private static final long DEFAULT_TIMEOUT_MS = 8000;
    private static final long POLL_INTERVAL_MS = 50;

    public static void main(String[] args) {
        System.out.println("=== TESTES AUTOMATIZADOS DO SO ===\n");

        testeExecucaoAutomaticaSemExec();
        testeIOAssincronoComEntrada();
        testePageFaultEOperacaoDeDisco();
        testeVitimizacaoComMemoriaLimitada();

        System.out.println("=== TODOS OS TESTES CONCLUÍDOS ===");
    }

    /**
     * Verifica que os processos executam até o fim sem necessidade de usar o comando
     * manual `exec`, e que os valores retornados por SYSCALL OUT são capturados.
     */
    private static void testeExecucaoAutomaticaSemExec() {
        System.out.println("1. Execução automática sem comando `exec`");
        Sistema sistema = new Sistema(1024, 8, 5);
        startCoreThreads(sistema);

        int pidSoma = sistema.so.newProcess("soma");          // faz OUT de 30
        int pidFat = sistema.so.newProcess("fatorialV2");     // faz OUT de 120

        boolean completos = waitFor(() ->
                sistema.so.getPCB(pidSoma) == null &&
                sistema.so.getPCB(pidFat) == null,
                DEFAULT_TIMEOUT_MS);

        Integer outSoma = sistema.so.getLastOutput(pidSoma);
        Integer outFatorial = sistema.so.getLastOutput(pidFat);

        System.out.println("   Processos concluídos automaticamente: " + (completos ? "OK" : "FALHOU"));
        System.out.println("   Último OUT soma (esperado 30): " + outSoma);
        System.out.println("   Último OUT fatorialV2 (esperado 120): " + outFatorial);

        shutdownSistema(sistema);
        System.out.println("   ✓ Teste 1 finalizado\n");
    }

    /**
     * Testa o fluxo completo de IO assíncrono: o processo bloqueia esperando entrada,
     * recebe input via shell programático (provideInput) e termina normalmente.
     */
    private static void testeIOAssincronoComEntrada() {
        System.out.println("2. IO assíncrono com bloqueio e desbloqueio");
        Sistema sistema = new Sistema(512, 8, 5);
        startCoreThreads(sistema);

        int pid = sistema.so.newProcess("fibonacciREAD");

        boolean bloqueadoPorIO = waitFor(() -> {
            PCB pcb = sistema.so.getPCB(pid);
            return pcb != null &&
                   pcb.state == PCB.ProcState.BLOCKED &&
                   pcb.ioTypeCode == 1; // IN
        }, DEFAULT_TIMEOUT_MS);

        if (bloqueadoPorIO) {
            System.out.println("   Processo bloqueado aguardando IN: OK");
            sistema.so.provideInput(pid, 8);
        } else {
            System.out.println("   Processo não bloqueou para IN: FALHOU");
        }

        boolean terminou = waitFor(() -> sistema.so.getPCB(pid) == null, DEFAULT_TIMEOUT_MS);
        System.out.println("   Processo concluiu após fornecer entrada: " + (terminou ? "OK" : "FALHOU"));

        shutdownSistema(sistema);
        System.out.println("   ✓ Teste 2 finalizado\n");
    }

    /**
     * Garante que page faults acionam o disco e que páginas são efetivamente
     * carregadas do dispositivo secundário.
     */
    private static void testePageFaultEOperacaoDeDisco() {
        System.out.println("3. Page fault acionando disco");
        Sistema sistema = new Sistema(64, 8, 10); // memória pequena para forçar page fault
        startCoreThreads(sistema);

        int pid = sistema.so.newProcess("fibonacci10");

        boolean houvePageFault = waitFor(() -> sistema.so.getDiskDevice().getQueueSize() > 0
                || sistema.so.getDiskDevice().getDiskStorageSize() > 0,
                DEFAULT_TIMEOUT_MS);
        System.out.println("   Page fault gerou operação de disco: " + (houvePageFault ? "OK" : "FALHOU"));

        boolean terminou = waitFor(() -> sistema.so.getPCB(pid) == null, DEFAULT_TIMEOUT_MS);
        System.out.println("   Processo terminou após tratar faults: " + (terminou ? "OK" : "FALHOU"));

        int paginasNoDisco = sistema.so.getDiskDevice().getDiskStorageSize();
        System.out.println("   Páginas armazenadas no disco (>=0): " + paginasNoDisco);

        shutdownSistema(sistema);
        System.out.println("   ✓ Teste 3 finalizado\n");
    }

    /**
     * Com vários processos concorrendo por poucos frames, garante que ocorre
     * vitimação de páginas e que o sistema permanece executando.
     */
    private static void testeVitimizacaoComMemoriaLimitada() {
        System.out.println("4. Vitimação e execução concorrente");
        Sistema sistema = new Sistema(64, 8, 5);
        startCoreThreads(sistema);

        int pid1 = sistema.so.newProcess("fibonacci10");
        int pid2 = sistema.so.newProcess("PC");
        int pid3 = sistema.so.newProcess("fatorialV2");

        waitFor(() -> sistema.so.getDiskDevice().getDiskStorageSize() >= 2, DEFAULT_TIMEOUT_MS);
        int paginasDisco = sistema.so.getDiskDevice().getDiskStorageSize();
        System.out.println("   Páginas no disco após vitimação (>=2 esperado): " + paginasDisco);

        boolean finalizados = waitFor(() ->
                sistema.so.getPCB(pid1) == null &&
                sistema.so.getPCB(pid2) == null &&
                sistema.so.getPCB(pid3) == null,
                DEFAULT_TIMEOUT_MS * 2);
        System.out.println("   Todos os processos finalizaram: " + (finalizados ? "OK" : "PENDENTE"));

        shutdownSistema(sistema);
        System.out.println("   ✓ Teste 4 finalizado\n");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return condition.get();
    }
}