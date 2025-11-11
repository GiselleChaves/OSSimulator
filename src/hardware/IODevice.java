package hardware;

import software.PCB;
import software.SO;
import software.PageFaultException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Dispositivo de IO (Console) - Thread separada que processa pedidos de IO
 * Modelo produtor/consumidor: CPU produz pedidos, IODevice consome
 */
public class IODevice implements Runnable {
    
    public enum IOType {
        READ, WRITE
    }
    
    /**
     * Representa um pedido de IO
     */
    public static class IORequest {
        public final IOType type;
        public final PCB process;
        public final int address; // Endereço lógico para leitura/escrita
        public int value;         // Valor resultante (para IN)
        private final ArrayBlockingQueue<Integer> pendingInput;

        private IORequest(IOType type, PCB process, int address, int value, boolean needsInput) {
            this.type = type;
            this.process = process;
            this.address = address;
            this.value = value;
            this.pendingInput = needsInput ? new ArrayBlockingQueue<>(1) : null;
        }

        public static IORequest read(PCB process, int address) {
            return new IORequest(IOType.READ, process, address, 0, true);
        }

        public static IORequest write(PCB process, int address) {
            return new IORequest(IOType.WRITE, process, address, 0, false);
        }

        public boolean needsInput() {
            return pendingInput != null;
        }

        public boolean provideInput(int input) {
            if (pendingInput == null) return false;
            return pendingInput.offer(input);
        }

        public int awaitInput() throws InterruptedException {
            if (pendingInput == null) {
                throw new IllegalStateException("Requisição não necessita entrada");
            }
            return pendingInput.take();
        }
    }
    
    private final BlockingQueue<IORequest> requestQueue;
    private final ConcurrentHashMap<Integer, IORequest> pendingReadRequests;
    private final SO so;
    private volatile boolean active;
    
    // Tempo de simulação de IO (em ms)
    private static final int IO_DELAY = 500;
    private static final int RETRY_DELAY = 200;
    
    public IODevice(SO so) {
        this.so = so;
        this.requestQueue = new LinkedBlockingQueue<>();
        this.pendingReadRequests = new ConcurrentHashMap<>();
        this.active = true;
    }
    
    /**
     * Adiciona um pedido de IO à fila
     */
    public void addRequest(IORequest request) {
        try {
            requestQueue.put(request);
            System.out.println("[IO] Pedido de " + request.type + " adicionado à fila (processo " + 
                             request.process.pid + ", endereço lógico " + request.address + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void run() {
        System.out.println("[IO] Thread de IO (Console) iniciada");
        
        while (active) {
            try {
                // Aguarda pedido na fila (bloqueante)
                IORequest request = requestQueue.take();
                
                if (!active) {
                    break;
                }
                
                processRequest(request);
                
            } catch (InterruptedException e) {
                if (!active) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("[IO] Thread de IO finalizada");
    }
    
    private void processRequest(IORequest request) throws InterruptedException {
        switch (request.type) {
            case READ:
                handleReadRequest(request);
                break;
            case WRITE:
                handleWriteRequest(request);
                break;
            default:
                System.out.println("[IO] Tipo de requisição desconhecido: " + request.type);
        }
    }

    private void handleReadRequest(IORequest request) throws InterruptedException {
        System.out.println(String.format("[IO] Processo %d solicitou IN no endereço lógico %d. Use: in %d <valor>",
                request.process.pid, request.address, request.process.pid));

        pendingReadRequests.put(request.process.pid, request);
        int value = request.awaitInput();
        pendingReadRequests.remove(request.process.pid);

        // Simula tempo de IO
        Thread.sleep(IO_DELAY);

        boolean done = false;
        while (!done) {
            try {
                int physicalAddr = so.traduzEndereco(request.process, request.address, true);
                Word dataWord = new Word(Opcode.DATA, -1, -1, value);
                so.hw.mem.write(physicalAddr, dataWord);
                System.out.println("[IO] READ concluído: valor " + value + 
                                 " escrito no endereço físico " + physicalAddr);
                done = true;
            } catch (PageFaultException e) {
                System.out.println("[IO] Página necessária para IN não está em memória. Aguardando carregamento...");
                Thread.sleep(RETRY_DELAY);
            }
        }

        so.hw.cpu.signalIOInterrupt(request.process);
    }

    private void handleWriteRequest(IORequest request) throws InterruptedException {
        // Simula tempo de IO
        Thread.sleep(IO_DELAY);

        boolean done = false;
        while (!done) {
            try {
                int physicalAddr = so.traduzEndereco(request.process, request.address, false);
                Word dataWord = so.hw.mem.read(physicalAddr);
                System.out.println("[IO] OUT (processo " + request.process.pid + "): " + dataWord.p);
                System.out.println("[IO] WRITE concluído: valor " + dataWord.p + 
                                 " do endereço físico " + physicalAddr);
                done = true;
            } catch (PageFaultException e) {
                System.out.println("[IO] Página necessária para OUT não está em memória. Aguardando carregamento...");
                Thread.sleep(RETRY_DELAY);
            }
        }

        so.hw.cpu.signalIOInterrupt(request.process);
    }
    
    public boolean provideInput(int pid, int value) {
        IORequest request = pendingReadRequests.get(pid);
        if (request == null) {
            return false;
        }
        boolean accepted = request.provideInput(value);
        if (!accepted) {
            System.out.println("[IO] Valor informado antes do dispositivo estar pronto. Tente novamente.");
        }
        return accepted;
    }
    
    public void shutdown() {
        active = false;
    }
    
    public int getQueueSize() {
        return requestQueue.size();
    }
}
