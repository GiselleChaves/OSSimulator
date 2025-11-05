package hardware;

import software.PCB;
import software.SO;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
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
        public IOType type;
        public PCB process;
        public int address; // Endereço lógico para leitura/escrita
        public int value;   // Valor para escrita (ignorado em leitura)
        
        public IORequest(IOType type, PCB process, int address, int value) {
            this.type = type;
            this.process = process;
            this.address = address;
            this.value = value;
        }
    }
    
    private BlockingQueue<IORequest> requestQueue;
    private SO so;
    private boolean active;
    private Scanner scanner;
    
    // Tempo de simulação de IO (em ms)
    private static final int IO_DELAY = 500;
    
    public IODevice(SO so) {
        this.so = so;
        this.requestQueue = new LinkedBlockingQueue<>();
        this.active = true;
        this.scanner = new Scanner(System.in);
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
                
                System.out.println("[IO] Processando " + request.type + " para processo " + 
                                 request.process.pid + "...");
                
                // Simula tempo de IO
                Thread.sleep(IO_DELAY);
                
                // Processa o pedido
                processRequest(request);
                
                // Gera interrupção para CPU sinalizar que IO terminou
                so.hw.cpu.signalIOInterrupt(request.process);
                
            } catch (InterruptedException e) {
                if (!active) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("[IO] Thread de IO finalizada");
    }
    
    private void processRequest(IORequest request) {
        try {
            // Traduz endereço lógico para físico usando DMA (acesso direto à memória)
            int physicalAddr = so.traduzEndereco(request.process, request.address);
            
            if (request.type == IOType.READ) {
                // Leitura: lê do console e escreve na memória
                System.out.print("[IO] Digite um valor inteiro para processo " + 
                               request.process.pid + ": ");
                int value = scanner.nextInt();
                scanner.nextLine(); // Consumir newline
                
                // DMA: escreve diretamente na memória
                Word dataWord = new Word(Opcode.DATA, -1, -1, value);
                so.hw.mem.write(physicalAddr, dataWord);
                
                System.out.println("[IO] READ concluído: valor " + value + 
                                 " escrito no endereço físico " + physicalAddr);
                
            } else if (request.type == IOType.WRITE) {
                // Escrita: lê da memória e escreve no console
                
                // DMA: lê diretamente da memória
                Word dataWord = so.hw.mem.read(physicalAddr);
                System.out.println("[IO] OUT (processo " + request.process.pid + "): " + dataWord.p);
                
                System.out.println("[IO] WRITE concluído: valor " + dataWord.p + 
                                 " do endereço físico " + physicalAddr);
            }
            
        } catch (Exception e) {
            System.out.println("[IO] ERRO ao processar pedido: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        active = false;
    }
    
    public int getQueueSize() {
        return requestQueue.size();
    }
}

