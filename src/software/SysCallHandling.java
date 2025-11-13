package software;

import hardware.IODevice;

// ------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
public class SysCallHandling {
    private SO so; // referencia ao SO

    public SysCallHandling(SO so) {
        this.so = so;
    }

    public void stop() { // chamada de sistema indicando final de programa
        System.out.println("SYSCALL STOP: Finalizando processo");
        so.terminateRunning("syscall_stop");
    }

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

            if (running.ioPending && running.ioCompleted) {
                running.ioPending = false;
                running.ioCompleted = false;
                running.ioTypeCode = 0;
                running.ioLogicalAddr = -1;
                return true;
            }

            if (running.ioPending && !running.ioCompleted) {
                so.scheduler.blockRunningProcess("io_waiting");
                return false;
            }

            running.ioPending = true;
            running.ioCompleted = false;
            running.ioTypeCode = ioType;
            running.ioLogicalAddr = address;

            so.hw.cpu.saveContext(running);
            
            IODevice.IORequest req = isRead ? IODevice.IORequest.read(running, address) : IODevice.IORequest.write(running, address);

            so.getIODevice().addRequest(req);
            so.scheduler.blockRunningProcess(isRead ? "io_request" : "io_request");
            
            return false;
        }

        System.out.println("[SYSCALL] PARAMETRO INVALIDO: " + ioType);
        return true;
        
    }
}
