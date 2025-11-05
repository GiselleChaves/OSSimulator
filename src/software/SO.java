package software;

import hardware.Hw;
import hardware.Word;
import hardware.Opcode;
import hardware.IODevice;
import hardware.DiskDevice;
import menagers.MemoryManager;
import program.Program;
import program.Programs;
import util.Utilities;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public Hw hw;
    
    // Dispositivo de IO
    private IODevice ioDevice;
    
    // Dispositivo de Disco (para paginação)
    private DiskDevice diskDevice;

    // Gerente de Memória (GM paginado)
    private MemoryManager memoryManager;

    // Gerente de Processos (GP)
    private Map<Integer, PCB> processTable;
    private AtomicInteger nextPid;

    // Escalonador
    public Scheduler scheduler;

    // Programas disponíveis
    private Programs programs;

    // Controle de trace global
    private boolean globalTrace;

    // Lock para operações thread-safe
    private ReentrantLock lock;

    public SO(Hw hw) {
        this.hw = hw;
        ih = new InterruptHandling(this); // rotinas de tratamento de int
        sc = new SysCallHandling(this); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        hw.cpu.setSO(this);
        utils = new Utilities(hw);
        
        // Inicializar dispositivo de IO
        ioDevice = new IODevice(this);
        
        // Inicializar dispositivo de Disco
        diskDevice = new DiskDevice(this);

        // Inicializar GM com parâmetros da memória
        memoryManager = new MemoryManager(hw.mem.getTamMem(), hw.mem.getTamPg());

        // Inicializar GP
        processTable = new HashMap<>();
        nextPid = new AtomicInteger(1);

        // Inicializar Escalonador
        scheduler = new Scheduler(this);

        // Programas
        programs = new Programs();

        globalTrace = false;
        lock = new ReentrantLock();
    }
    
    public IODevice getIODevice() {
        return ioDevice;
    }
    
    public DiskDevice getDiskDevice() {
        return diskDevice;
    }

    // ============== GERENTE DE MEMÓRIA (GM PAGINADO) ==============

    public boolean gmAloca(int nroPalavras, PCB pcb) {
        int numPages = (int) Math.ceil((double) nroPalavras / hw.mem.getTamPg());
        pcb.numPages = numPages;
        
        // MEMÓRIA VIRTUAL: Carregar apenas a primeira página
        System.out.println("GM: Alocando primeira página para processo " + pcb.pid +
                " (" + nroPalavras + " palavras, " + numPages + " páginas totais)");
        
        // Alocar frame apenas para primeira página
        int frame = memoryManager.allocateFrame(pcb, 0);
        if (frame < 0) {
            System.out.println("ERRO: Não foi possível alocar frame para primeira página do processo " + pcb.pid);
            return false;
        }
        
        // Configurar primeira página como válida
        pcb.pageTable[0].frameNumber = frame;
        pcb.pageTable[0].valid = true;
        pcb.pageTable[0].lastAccessTime = System.currentTimeMillis();
        
        System.out.println("GM: Primeira página (pg0) alocada no frame " + frame);
        System.out.println("    Demais páginas serão carregadas sob demanda (page fault)");

        return true;
    }

    public void gmDesaloca(PCB pcb) {
        if (pcb.pageTable != null) {
            // Desalocar todos os frames em uso
            for (int i = 0; i < pcb.pageTable.length; i++) {
                if (pcb.pageTable[i].valid && pcb.pageTable[i].frameNumber >= 0) {
                    memoryManager.deallocateFrame(pcb.pageTable[i].frameNumber);
                }
            }
            System.out.println("GM: Desalocada memória do processo " + pcb.pid);
            pcb.pageTable = null;
        }
    }

    public int traduzEndereco(PCB pcb, int endLogico) {
        if (pcb.pageTable == null) {
            throw new RuntimeException("Processo " + pcb.pid + " não tem tabela de páginas");
        }

        int tamPg = hw.mem.getTamPg();
        int pagina = endLogico / tamPg;
        int offset = endLogico % tamPg;

        if (pagina >= pcb.pageTable.length) {
            throw new RuntimeException("Acesso à página " + pagina + " inválida para processo " + pcb.pid);
        }

        PageTableEntry entry = pcb.pageTable[pagina];
        
        // PAGE FAULT: página não está em memória
        if (!entry.valid) {
            System.out.println("[PAGE_FAULT] Processo " + pcb.pid + " acessou página " + pagina + " não carregada");
            handlePageFault(pcb, pagina);
            // Após tratamento, entry deve estar válida
        }
        
        // Atualizar tempo de acesso (para LRU)
        entry.lastAccessTime = System.currentTimeMillis();
        
        int frame = entry.frameNumber;
        int endFisico = frame * tamPg + offset;

        if (globalTrace || pcb.trace) {
            System.out.println("Tradução: endLog=" + endLogico + " → pg=" + pagina +
                    ", offset=" + offset + ", frame=" + frame + " → endFis=" + endFisico);
        }

        return endFisico;
    }
    
    /**
     * Tratamento de page fault - ASSÍNCRONO com bloqueio de processo
     */
    private void handlePageFault(PCB pcb, int pageNumber) {
        System.out.println("[PAGE_FAULT] Tratando page fault para processo " + pcb.pid + ", página " + pageNumber);
        
        PageTableEntry entry = pcb.pageTable[pageNumber];
        
        // Tentar alocar frame
        int frame = memoryManager.allocateFrame(pcb, pageNumber);
        
        // Se não há frame livre, precisa vitimar uma página (SÍNCRONO - vitimação é rápida)
        if (frame < 0) {
            System.out.println("[PAGE_FAULT] Sem frames livres, selecionando vítima...");
            frame = evictPageSync(pcb);
        }
        
        if (frame < 0) {
            throw new RuntimeException("ERRO: Não foi possível alocar frame para page fault");
        }
        
        // ASSÍNCRONO: Envia pedido de carga de página para o disco
        System.out.println("[PAGE_FAULT] Enviando pedido de carga de página ao disco...");
        DiskDevice.DiskOperation operation = new DiskDevice.DiskOperation(
            DiskDevice.DiskOpType.LOAD_PAGE,
            pcb,
            pageNumber,
            frame,
            entry.diskAddress
        );
        
        diskDevice.addOperation(operation);
        
        // BLOQUEAR processo até disco terminar (será desbloqueado na interrupção de disco)
        System.out.println("[PAGE_FAULT] Bloqueando processo " + pcb.pid + " até carga completar");
        pcb.state = PCB.ProcState.BLOCKED;
        scheduler.blockRunningProcess();
        
        // CPU continuará com outro processo enquanto disco carrega a página
    }
    
    /**
     * Evita uma página (política de vítimas) - versão SÍNCRONA
     * Usado durante page fault para liberar frame rapidamente
     */
    private int evictPageSync(PCB requestingPCB) {
        int victimFrame = memoryManager.selectVictim();
        if (victimFrame < 0) {
            return -1;
        }
        
        MemoryManager.FrameInfo info = memoryManager.getFrameOwner(victimFrame);
        if (info == null) {
            return victimFrame;
        }
        
        PCB victimPCB = info.owner;
        int victimPage = info.pageNumber;
        
        System.out.println("[EVICT] Vitimando página " + victimPage + " do processo " + 
                         victimPCB.pid + " (frame " + victimFrame + ")");
        
        PageTableEntry victimEntry = victimPCB.pageTable[victimPage];
        
        // SALVAR página no disco (via DiskDevice)
        // Salvamento é síncrono aqui para simplificar (na prática seria assíncrono também)
        System.out.println("[EVICT] Salvando página vitimada no disco...");
        int diskAddr = diskDevice.savePage(victimPCB, victimPage, victimFrame);
        
        // Atualizar entrada da tabela de páginas da vítima
        victimEntry.valid = false;
        victimEntry.frameNumber = -1;
        victimEntry.diskAddress = diskAddr;
        victimEntry.modified = false; // Página foi salva, não está mais modificada
        
        // Liberar frame
        memoryManager.deallocateFrame(victimFrame);
        
        System.out.println("[EVICT] Frame " + victimFrame + " liberado, página salva no disco (addr=" + diskAddr + ")");
        
        return victimFrame;
    }

    // Carregamento de programa - agora carrega apenas primeira página
    public void carregaPrograma(Program programa, PCB pcb) {
        Word[] programImage = programa.image;
        int tamPg = hw.mem.getTamPg();
        
        // Carregar apenas primeira página (lazy loading)
        int endLogIni = 0;
        int endLogFim = Math.min(tamPg - 1, programImage.length - 1);
        
        carregaPagina(pcb, 0, pcb.pageTable[0].frameNumber, programa);
        
        System.out.println("Programa '" + programa.name + "' - primeira página carregada para processo " + pcb.pid);
        System.out.println("  Demais páginas serão carregadas sob demanda");
    }
    
    /**
     * Carrega uma página específica do programa na memória
     */
    private void carregaPagina(PCB pcb, int pageNumber, int frameNumber) {
        // Buscar programa original
        Program programa = null;
        for (Program p : programs.progs) {
            if (p != null && p.name.equals(pcb.nome)) {
                programa = p;
                break;
            }
        }
        
        if (programa == null) {
            System.out.println("ERRO: Programa '" + pcb.nome + "' não encontrado");
            return;
        }
        
        carregaPagina(pcb, pageNumber, frameNumber, programa);
    }
    
    /**
     * Carrega uma página específica do programa na memória
     */
    private void carregaPagina(PCB pcb, int pageNumber, int frameNumber, Program programa) {
        Word[] programImage = programa.image;
        int tamPg = hw.mem.getTamPg();
        
        int endLogIni = pageNumber * tamPg;
        int endLogFim = Math.min(endLogIni + tamPg - 1, programImage.length - 1);
        
        int endFisBase = frameNumber * tamPg;
        
        for (int logAddr = endLogIni; logAddr <= endLogFim; logAddr++) {
            int offset = logAddr - endLogIni;
            int physAddr = endFisBase + offset;
            hw.mem.write(physAddr, programImage[logAddr]);
        }
        
        // Preencher resto da página com NOPs se necessário
        for (int offset = (endLogFim - endLogIni + 1); offset < tamPg; offset++) {
            int physAddr = endFisBase + offset;
            hw.mem.write(physAddr, new Word(Opcode.DATA, 0, 0, 0));
        }
    }
    
    /**
     * Método público para DiskDevice carregar página do programa original
     * Chamado quando página nunca foi carregada antes (diskAddress < 0)
     */
    public void carregaPaginaFromDisk(PCB pcb, int pageNumber, int frameNumber) {
        // Buscar programa original
        Program programa = null;
        for (Program p : programs.progs) {
            if (p != null && p.name.equals(pcb.nome)) {
                programa = p;
                break;
            }
        }
        
        if (programa == null) {
            System.out.println("ERRO: Programa '" + pcb.nome + "' não encontrado");
            return;
        }
        
        carregaPagina(pcb, pageNumber, frameNumber, programa);
    }

    private int computeRequiredWords(Program programa) {
        int needed = programa.image.length;
        for (Word w : programa.image) {
            if (w == null) continue;
            switch (w.opc) {
                case LDD: case STD: case JMP: case JMPIM: case JMPIGK: case JMPILK: case JMPIEK:
                case JMPIGM: case JMPILM: case JMPIEM:
                    if (w.p >= 0) needed = Math.max(needed, w.p + 1);
                    break;
                default:
                    break;
            }
        }
        return needed;
    }

    // ============== GERENTE DE PROCESSOS (GP) ==============

    public int newProcess(String nomeProg) {
        lock.lock();
        try {
            Program programa = null;
            for (Program p : programs.progs) {
                if (p != null && p.name.equals(nomeProg)) {
                    programa = p;
                    break;
                }
            }

            if (programa == null) {
                System.out.println("ERRO: Programa '" + nomeProg + "' não encontrado");
                return -1;
            }

            int requiredWords = computeRequiredWords(programa);

            int pid = nextPid.getAndIncrement();
            PCB pcb = new PCB(pid, nomeProg, requiredWords, hw.mem.getTamPg());

            // Alocar memória para código + dados
            if (!gmAloca(requiredWords, pcb)) {
                return -1;
            }

            // Carregar programa
            carregaPrograma(programa, pcb);

            // Adicionar à tabela de processos
            processTable.put(pid, pcb);

            // Colocar na fila READY (mas não executar automaticamente)
            scheduler.addToReady(pcb);

            System.out.println("Processo criado: pid=" + pid + ", nome=" + nomeProg +
                    ", tamanho=" + requiredWords + " palavras (image=" + programa.image.length + ")");

            return pid;
        } finally {
            lock.unlock();
        }
    }

    public boolean rm(int pid) {
        lock.lock();
        try {
            PCB pcb = processTable.get(pid);
            if (pcb == null) {
                System.out.println("ERRO: Processo " + pid + " não existe");
                return false;
            }

            // Remover do escalonador
            scheduler.removeProcess(pid);

            // Desalocar memória
            gmDesaloca(pcb);

            // Remover da tabela de processos
            processTable.remove(pid);

            System.out.println("Processo " + pid + " removido");
            return true;
        } finally {
            lock.unlock();
        }
    }

    public List<PCB> ps() {
        lock.lock();
        try {
            return new ArrayList<>(processTable.values());
        } finally {
            lock.unlock();
        }
    }

    public String dump(int pid) {
        lock.lock();
        try {
            PCB pcb = processTable.get(pid);
            if (pcb == null) {
                return "ERRO: Processo " + pid + " não existe";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== DUMP PROCESSO ").append(pid).append(" ===\n");
            sb.append(pcb.toString()).append("\n");

            // Mostrar mapeamento lógico → físico
            sb.append("Mapeamento memória:\n");
            for (int pg = 0; pg < pcb.numPages; pg++) {
                PageTableEntry entry = pcb.pageTable[pg];
                int endLogIni = pg * hw.mem.getTamPg();
                int endLogFim = Math.min(endLogIni + hw.mem.getTamPg() - 1, pcb.tamanhoEmPalavras - 1);
                
                if (entry.valid) {
                    int frame = entry.frameNumber;
                    int endFisIni = frame * hw.mem.getTamPg();
                    int endFisFim = endFisIni + hw.mem.getTamPg() - 1;
                    sb.append(String.format("  Página %d (end.lóg %d-%d) → Frame %d (end.fís %d-%d) [VALID]\n",
                            pg, endLogIni, endLogFim, frame, endFisIni, endFisFim));
                } else {
                    sb.append(String.format("  Página %d (end.lóg %d-%d) → NOT IN MEMORY\n",
                            pg, endLogIni, endLogFim));
                }
            }

            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    public String frames() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== FRAMES (0=livre, 1=ocupado) ===\n");
        boolean[] v = memoryManager.getFrames();
        int livres = 0, ocupados = 0;
        for (boolean b : v) { if (b) ocupados++; else livres++; }
        sb.append(String.format("Total: %d | Livres: %d | Ocupados: %d | tamPg=%d\n\n", v.length, livres, ocupados, hw.mem.getTamPg()));
        for (int i = 0; i < v.length; i++) {
            int ini = i * hw.mem.getTamPg();
            int fim = ini + hw.mem.getTamPg() - 1;
            sb.append(String.format("frame %3d: [%4d..%4d]  %s\n", i, ini, fim, v[i] ? "1" : "0"));
        }
        return sb.toString();
    }

    public String dumpM(int ini, int fim) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DUMP MEMÓRIA FÍSICA ").append(ini).append("-").append(fim).append(" ===\n");

        for (int i = ini; i <= fim && i < hw.mem.getTamMem(); i++) {
            Word w = hw.mem.read(i);
            sb.append(String.format("%4d: %s\n", i, w.toString()));
        }

        return sb.toString();
    }

    // ============== CONTROLE DE EXECUÇÃO ==============

    public void exec(int pid) {
        lock.lock();
        try {
            PCB pcb = processTable.get(pid);
            if (pcb == null) {
                System.out.println("ERRO: Processo " + pid + " não existe");
                return;
            }

            System.out.println("Executando processo " + pid);

            // Remover do escalonador se estiver lá
            scheduler.removeProcess(pid);

            // Execução normal: com preempção
            hw.cpu.setPreemptive(true);
            hw.cpu.setContext(pcb);
            pcb.state = PCB.ProcState.RUNNING;

            // Execução até terminar ou dar erro
            int maxSteps = 1000; // Limite para evitar loop infinito
            int steps = 0;

            while (pcb.state == PCB.ProcState.RUNNING && steps < maxSteps) {
                hw.cpu.step();
                hw.cpu.saveContext(pcb);
                steps++;

                // Verificar se processo terminou por STOP ou erro
                if (pcb.state == PCB.ProcState.TERMINATED) {
                    break;
                }

                // Pequena pausa para evitar loop muito rápido
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (steps >= maxSteps) {
                System.out.println("AVISO: Execução interrompida após " + maxSteps + " passos");
            }

            System.out.println("Execução do processo " + pid + " finalizada (estado: " + pcb.state + ")");
            
            // Se o processo terminou, removê-lo
            if (pcb.state == PCB.ProcState.TERMINATED) {
                rm(pid);
            } else {
                // Se não terminou, colocar de volta na fila READY
                scheduler.addToReady(pcb);
            }
        } finally {
            lock.unlock();
        }
    }

    public void execAll() {
        System.out.println("Iniciando execução escalonada de todos os processos...");

        // Ativar escalonamento automático
        scheduler.setAutoSchedule(true);

        // Aguardar até todos os processos terminarem
        while (scheduler.hasReadyProcesses()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Desativar escalonamento automático
        scheduler.setAutoSchedule(false);
        System.out.println("Todos os processos finalizaram");
    }

    // ============== CONTROLE DE CONTEXTO ==============

    public void setContext(PCB pcb) {
        hw.cpu.setContext(pcb);
    }

    public void saveContext(PCB pcb) {
        hw.cpu.saveContext(pcb);
    }

    // ============== CONTROLE DE TRACE ==============

    public void traceOn() {
        globalTrace = true;
        System.out.println("Trace global ativado");
    }

    public void traceOff() {
        globalTrace = false;
        System.out.println("Trace global desativado");
    }

    // ============== GETTERS ==============

    public PCB getPCB(int pid) {
        return processTable.get(pid);
    }

    public Programs getPrograms() {
        return programs;
    }

    public boolean isGlobalTrace() {
        return globalTrace;
    }
}
