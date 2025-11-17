package software;

import hardware.IODevice;

// ------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
/**
 * Lida com `SYSCALL` de IO disparada por programas:
 * - r8 define o tipo: 1=IN (leitura), 2=OUT (escrita)
 * - r9 define o endereço lógico alvo
 *
 * Fluxo: salva contexto → cria requisição → bloqueia o processo → CPU segue com outro.
 * No retorno (interrupção de IO) o processo é desbloqueado e continua do PC+1.
 */
public class SysCallHandling {
    private SO so; // referencia ao SO

    public SysCallHandling(SO so) {
        this.so = so;
    }

    public void stop() { // chamada de sistema indicando final de programa
        System.out.println("SYSCALL STOP: Finalizando processo");
        so.terminateRunning("syscall_stop");
    }

    /**
     * Trata a chamada de sistema de IO.
     * Retorna true se a instrução pode avançar PC+1 imediatamente (casos inválidos
     * ou retorno já concluído), e false se o processo foi bloqueado para aguardar.
     */
    public boolean handle() {
        int ioType = so.hw.cpu.getReg()[8];
        int address = so.hw.cpu.getReg()[9];
        
        System.out.println("[SYSCALL] IO tipo: " + ioType + " / endereço lógico: " + address);

        PCB running = so.scheduler.getRunning();
        if (running == null) {
            return true;
        }

        if (ioType == 1 || ioType == 2) {
            boolean isRead = (ioType == 1);

            if (running.ioPending && !running.ioCompleted) {
                so.scheduler.blockRunningProcess("io");
                return false;
            }

            if (running.ioPending && running.ioCompleted) {
                running.ioPending = false;
                running.ioCompleted = false;
                running.ioTypeCode = 0;
                running.ioLogicalAddr = -1;
                return true;
            }

            // Garante que a página está residente (pode disparar page fault)
            so.traduzEndereco(running, address, isRead);

            running.ioPending = true;
            running.ioCompleted = false;
            running.ioTypeCode = ioType;
            running.ioLogicalAddr = address;

            so.hw.cpu.saveContext(running);

            IODevice.IORequest req = isRead ? IODevice.IORequest.read(running, address)
                                            : IODevice.IORequest.write(running, address);

            so.getIODevice().addRequest(req);
            so.scheduler.blockRunningProcess("io");
            running.pc = running.pc + 1;

            return false;
        }

        System.out.println("[SYSCALL] PARAMETRO INVALIDO: " + ioType);
        return true;
        
    }
}
