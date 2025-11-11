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
        PCB running = so.scheduler.getRunning();
        if (running != null) {
            PCB.ProcState from = running.state;
            running.state = PCB.ProcState.TERMINATED;
            so.logStateChange(running, "syscall_stop", from, PCB.ProcState.TERMINATED);
            so.scheduler.removeProcess(running.pid);
            so.gmDesaloca(running);
            System.out.println("Processo " + running.pid + " finalizado via SYSCALL STOP");
            
            // Escalonar próximo processo
            so.scheduler.scheduleNext();
        }
    }

    public boolean handle() throws PageFaultException { // chamada de sistema
        // suporta somente IO, com parametros
        // reg[8] = in ou out    e reg[9] endereco do inteiro
        int ioType = so.hw.cpu.getReg()[8];
        int address = so.hw.cpu.getReg()[9];
        
        System.out.println("[SYSCALL] IO tipo: " + ioType + " / endereço lógico: " + address);

        PCB running = so.scheduler.getRunning();
        if (running == null) {
            System.out.println("[SYSCALL] ERRO: Nenhum processo em execução");
            return true;
        }

        boolean isWrite = (ioType == 1); // leitura gera escrita na memória do processo
        // Garante que a página está presente antes de enfileirar o pedido de IO
        so.traduzEndereco(running, address, isWrite);

        if (ioType == 1) {
            // IN - leitura do console
            System.out.println("[SYSCALL] IN solicitado por processo " + running.pid);
            
            // Salvar contexto
            so.hw.cpu.saveContext(running);
            
            // Criar pedido de IO
            IODevice.IORequest request = IODevice.IORequest.read(running, address);
            
            // Adicionar à fila do dispositivo de IO
            so.getIODevice().addRequest(request);
            
            // Bloquear processo
            so.scheduler.blockRunningProcess("io_request");
            
            return true;
        } else if (ioType == 2) {
            // OUT - escrita no console
            System.out.println("[SYSCALL] OUT solicitado por processo " + running.pid);
            
            // Salvar contexto
            so.hw.cpu.saveContext(running);
            
            // Criar pedido de IO (o valor será lido da memória pelo dispositivo)
            IODevice.IORequest request = IODevice.IORequest.write(running, address);
            
            // Adicionar à fila do dispositivo de IO
            so.getIODevice().addRequest(request);
            
            // Bloquear processo
            so.scheduler.blockRunningProcess("io_request");
            
            return true;
        } else {
            System.out.println("[SYSCALL] PARAMETRO INVALIDO: " + ioType);
            return true;
        }
    }
}
