package hardware;

import software.PCB;
import software.SO;
import software.PageTableEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Dispositivo de Disco - Thread separada que processa operações de paginação
 * Responsável por:
 * - Salvar páginas vitimadas da memória para disco
 * - Carregar páginas do disco de volta para memória
 * - Manter conteúdo de páginas que foram vitimadas
 */
public class DiskDevice implements Runnable {
    
    public enum DiskOpType {
        SAVE_PAGE,    // Salvar página vitimada no disco
        LOAD_PAGE     // Carregar página do disco para memória
    }
    
    /**
     * Representa uma operação de disco
     */
    public static class DiskOperation {
        public DiskOpType type;
        public PCB process;
        public int pageNumber;
        public int frameNumber;  // Frame destino (para LOAD) ou origem (para SAVE)
        public int diskAddress;  // Endereço no disco
        
        public DiskOperation(DiskOpType type, PCB process, int pageNumber, int frameNumber, int diskAddress) {
            this.type = type;
            this.process = process;
            this.pageNumber = pageNumber;
            this.frameNumber = frameNumber;
            this.diskAddress = diskAddress;
        }
    }
    
    private BlockingQueue<DiskOperation> operationQueue;
    private SO so;
    private boolean active;
    
    // Simula armazenamento em disco: diskAddress -> conteúdo da página
    private Map<Integer, Word[]> diskStorage;
    private int nextDiskAddress;
    
    // Tempo de simulação de operação de disco (em ms)
    private static final int DISK_DELAY = 300;
    
    public DiskDevice(SO so) {
        this.so = so;
        this.operationQueue = new LinkedBlockingQueue<>();
        this.diskStorage = new HashMap<>();
        this.nextDiskAddress = 0;
        this.active = true;
    }
    
    /**
     * Adiciona uma operação de disco à fila
     */
    public void addOperation(DiskOperation operation) {
        try {
            operationQueue.put(operation);
            System.out.println("[DISK] Operação de " + operation.type + " adicionada à fila (processo " + 
                             operation.process.pid + ", página " + operation.pageNumber + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Salva uma página da memória para o disco
     * Retorna o endereço no disco onde foi salva
     */
    public int savePage(PCB process, int pageNumber, int frameNumber) {
        System.out.println("[DISK] Salvando página " + pageNumber + " do processo " + 
                         process.pid + " (frame " + frameNumber + ")");
        
        // Ler conteúdo do frame da memória
        int tamPg = so.hw.mem.getTamPg();
        int physicalBase = frameNumber * tamPg;
        Word[] pageContent = new Word[tamPg];
        
        for (int i = 0; i < tamPg; i++) {
            pageContent[i] = so.hw.mem.read(physicalBase + i);
        }
        
        // Salvar no disco
        int diskAddr = nextDiskAddress++;
        diskStorage.put(diskAddr, pageContent);
        
        System.out.println("[DISK] Página salva no disco com endereço " + diskAddr);
        return diskAddr;
    }
    
    /**
     * Carrega uma página do disco para a memória
     */
    public void loadPage(int diskAddress, int frameNumber) {
        System.out.println("[DISK] Carregando página do disco (endereço " + diskAddress + 
                         ") para frame " + frameNumber);
        
        Word[] pageContent = diskStorage.get(diskAddress);
        if (pageContent == null) {
            System.out.println("[DISK] ERRO: Página não encontrada no disco (endereço " + diskAddress + ")");
            return;
        }
        
        // Escrever no frame da memória
        int tamPg = so.hw.mem.getTamPg();
        int physicalBase = frameNumber * tamPg;
        
        for (int i = 0; i < tamPg; i++) {
            so.hw.mem.write(physicalBase + i, pageContent[i]);
        }
        
        System.out.println("[DISK] Página carregada com sucesso no frame " + frameNumber);
    }
    
    /**
     * Carrega página original do programa para a memória
     */
    public void loadProgramPage(PCB process, int pageNumber, int frameNumber) {
        System.out.println("[DISK] Carregando página " + pageNumber + " do programa '" + 
                         process.nome + "' para frame " + frameNumber);
        
        // Usar método do SO para carregar página do programa original
        so.carregaPaginaFromDisk(process, pageNumber, frameNumber);
    }
    
    @Override
    public void run() {
        System.out.println("[DISK] Thread de Disco iniciada");
        
        while (active) {
            try {
                // Aguarda operação na fila (bloqueante)
                DiskOperation operation = operationQueue.take();
                
                System.out.println("[DISK] Processando " + operation.type + " para processo " + 
                                 operation.process.pid + ", página " + operation.pageNumber + "...");
                
                // Simula tempo de acesso ao disco
                Thread.sleep(DISK_DELAY);
                
                // Processa a operação
                processOperation(operation);
                
                // Gera interrupção para CPU sinalizar que operação de disco terminou
                so.hw.cpu.signalDiskInterrupt(operation);
                
            } catch (InterruptedException e) {
                if (!active) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("[DISK] Thread de Disco finalizada");
    }
    
    private void processOperation(DiskOperation operation) {
        try {
            if (operation.type == DiskOpType.SAVE_PAGE) {
                // Salvar página da memória para disco
                int diskAddr = savePage(operation.process, operation.pageNumber, operation.frameNumber);
                
                // Atualizar tabela de páginas do processo
                PageTableEntry entry = operation.process.pageTable[operation.pageNumber];
                entry.diskAddress = diskAddr;
                entry.valid = false;
                entry.frameNumber = -1;
                
                System.out.println("[DISK] SAVE_PAGE concluído: página " + operation.pageNumber + 
                                 " do processo " + operation.process.pid + " salva no disco (addr=" + diskAddr + ")");
                
            } else if (operation.type == DiskOpType.LOAD_PAGE) {
                // Carregar página do disco para memória
                PageTableEntry entry = operation.process.pageTable[operation.pageNumber];
                
                if (entry.diskAddress >= 0) {
                    // Página já foi vitimada antes, carregar do disco
                    loadPage(entry.diskAddress, operation.frameNumber);
                } else {
                    // Página nunca foi carregada, carregar do programa original
                    loadProgramPage(operation.process, operation.pageNumber, operation.frameNumber);
                }
                
                // Atualizar tabela de páginas
                entry.frameNumber = operation.frameNumber;
                entry.valid = true;
                entry.modified = false;
                entry.lastAccessTime = System.currentTimeMillis();
                
                so.onPageLoaded(operation.process, operation.pageNumber, operation.frameNumber);
                
                System.out.println("[DISK] LOAD_PAGE concluído: página " + operation.pageNumber + 
                                 " do processo " + operation.process.pid + " carregada no frame " + 
                                 operation.frameNumber);
            }
            
        } catch (Exception e) {
            System.out.println("[DISK] ERRO ao processar operação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void shutdown() {
        active = false;
    }
    
    public int getQueueSize() {
        return operationQueue.size();
    }
    
    /**
     * Para testes: retorna tamanho do armazenamento em disco
     */
    public int getDiskStorageSize() {
        return diskStorage.size();
    }
}

