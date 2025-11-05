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
        int ioType = so.hw.cpu.getReg()[8];
        int address = so.hw.cpu.getReg()[9];
        
        System.out.println("[SYSCALL] IO tipo: " + ioType + " / endereço lógico: " + address);

        PCB running = so.scheduler.getRunning();
        if (running == null) {
            System.out.println("[SYSCALL] ERRO: Nenhum processo em execução");
            return;
        }

        if (ioType == 1) {
            // IN - leitura do console
            System.out.println("[SYSCALL] IN solicitado por processo " + running.pid);
            
            // Salvar contexto
            so.hw.cpu.saveContext(running);
            
            // Criar pedido de IO
            IODevice.IORequest request = new IODevice.IORequest(
                IODevice.IOType.READ, running, address, 0
            );
            
            // Adicionar à fila do dispositivo de IO
            so.getIODevice().addRequest(request);
            
            // Bloquear processo
            so.scheduler.blockRunningProcess();
            
            // Escalonar próximo processo
            so.scheduler.scheduleNext();
            
        } else if (ioType == 2) {
            // OUT - escrita no console
            System.out.println("[SYSCALL] OUT solicitado por processo " + running.pid);
            
            // Salvar contexto
            so.hw.cpu.saveContext(running);
            
            // Criar pedido de IO (o valor será lido da memória pelo dispositivo)
            IODevice.IORequest request = new IODevice.IORequest(
                IODevice.IOType.WRITE, running, address, 0
            );
            
            // Adicionar à fila do dispositivo de IO
            so.getIODevice().addRequest(request);
            
            // Bloquear processo
            so.scheduler.blockRunningProcess();
            
            // Escalonar próximo processo
            so.scheduler.scheduleNext();
            
        } else {
            System.out.println("[SYSCALL] PARAMETRO INVALIDO: " + ioType);
        }
    }
}
