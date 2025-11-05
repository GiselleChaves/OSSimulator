// -------------------------------------------------------------------------------------------------------
// ------------------- S I S T E M A
// --------------------------------------------------------------------

import hardware.Hw;
import software.SO;
import software.Shell;

public class Sistema {
    public Hw hw;
    public SO so;
    public Shell shell;
    
    private Thread shellThread;
    private Thread schedulerThread;
    private Thread cpuThread;
    private Thread ioThread;
    private Thread diskThread;

    public Sistema(int tamMem) {
        this(tamMem, 8, 5); // defaults: tamPg=8, delta=5
    }
    
    public Sistema(int tamMem, int tamPg, int delta) {
        hw = new Hw(tamMem, tamPg, delta);           // memoria do HW tem tamMem palavras
        so = new SO(hw);
        shell = new Shell(so);
        
        System.out.println("Sistema inicializado:");
        System.out.println("  Memória: " + tamMem + " palavras");
        System.out.println("  Tamanho da página: " + tamPg + " palavras");
        System.out.println("  Delta (fatia tempo): " + delta + " instruções");
        System.out.println("  Frames disponíveis: " + hw.mem.getNroFrames());
    }

    public void run() {
        System.out.println("Iniciando threads do sistema...");
        
        // Iniciar thread do escalonador
        schedulerThread = new Thread(so.scheduler, "Scheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
        
        // Iniciar thread da CPU
        cpuThread = new Thread(hw.cpu, "CPU");
        cpuThread.setDaemon(true);
        cpuThread.start();
        
        // Iniciar thread de IO
        ioThread = new Thread(so.getIODevice(), "IODevice");
        ioThread.setDaemon(true);
        ioThread.start();
        
        // Iniciar thread de Disco
        diskThread = new Thread(so.getDiskDevice(), "DiskDevice");
        diskThread.setDaemon(true);
        diskThread.start();
        
        // Iniciar thread do shell (thread principal)
        shellThread = new Thread(shell, "Shell");
        shellThread.start();
        
        // Aguardar finalização do shell
        try {
            shellThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Finalizar outras threads
        so.scheduler.shutdown();
        hw.cpu.stopCPU();
        so.getIODevice().shutdown();
        so.getDiskDevice().shutdown();
        
        try {
            if (schedulerThread.isAlive()) {
                schedulerThread.interrupt();
                schedulerThread.join(1000);
            }
            if (cpuThread.isAlive()) {
                cpuThread.interrupt();
                cpuThread.join(1000);
            }
            if (ioThread.isAlive()) {
                ioThread.interrupt();
                ioThread.join(1000);
            }
            if (diskThread.isAlive()) {
                diskThread.interrupt();
                diskThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Sistema finalizado");
    }
}