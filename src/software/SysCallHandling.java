package software;

// ------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
public class SysCallHandling {
    private SO so; // referencia ao SO

    public SysCallHandling(SO so) {
        this.so = so;
    }

    public void stop() { // chamada de sistema indicando final de programa
        System.out.println("SYSCALL STOP: Finalizando processo");
        PCB running = so.scheduler.getRunning();
        if (running != null) {
            running.state = PCB.ProcState.TERMINATED;
            so.scheduler.removeProcess(running.pid);
            so.gmDesaloca(running);
            System.out.println("Processo " + running.pid + " finalizado via SYSCALL STOP");
            
            // Escalonar próximo processo
            so.scheduler.scheduleNext();
        }
    }

    public void handle() { // chamada de sistema
        // suporta somente IO, com parametros
        // reg[8] = in ou out    e reg[9] endereco do inteiro
        System.out.println("SYSCALL para:  " + so.hw.cpu.getReg()[8] + " / " + so.hw.cpu.getReg()[9]);

        if (so.hw.cpu.getReg()[8] == 1) {
            // leitura ...
            System.out.println("SYSCALL: Operação de leitura não implementada");
        } else if (so.hw.cpu.getReg()[8] == 2) {
            // escrita - escreve o conteudo da memoria na posicao dada em reg[9]
            PCB running = so.scheduler.getRunning();
            if (running != null) {
                int endLogico = so.hw.cpu.getReg()[9];
                try {
                    int endFisico = so.traduzEndereco(running, endLogico);
                    System.out.println("OUT: " + so.hw.mem.read(endFisico).p);
                } catch (Exception e) {
                    System.out.println("ERRO na SYSCALL: " + e.getMessage());
                }
            }
        } else {
            System.out.println("SYSCALL: PARAMETRO INVALIDO");
        }
    }
}
