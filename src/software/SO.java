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
import java.nio.file.Path;
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

    // Logger de mudanças de estado
    private StateLogger stateLogger;

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

        // Logger
        stateLogger = new StateLogger(Path.of("logs", "so_state.log"));

        globalTrace = false;
        lock = new ReentrantLock();
    }
    
    public IODevice getIODevice() {
        return ioDevice;
    }
    
    public DiskDevice getDiskDevice() {
        return diskDevice;
    }

    public boolean provideInput(int pid, int value) {
        lock.lock();
        try {
            PCB pcb = processTable.get(pid);
            if (pcb == null) {
                System.out.println("ERRO: Processo " + pid + " não existe para receber entrada");
                return false;
            }
            boolean accepted = ioDevice.provideInput(pid, value);
            if (!accepted) {
                System.out.println("ERRO: Não há operação de IN pendente para o processo " + pid);
            }
            return accepted;
        } finally {
            lock.unlock();
        }
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

    public int traduzEndereco(PCB pcb, int endLogico, boolean isWrite) {
        lock.lock();
        try {
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
                throw new PageFaultException(pcb.pid, pagina, "Página " + pagina + " não está em memória física");
            }

            // Atualizar metadados da página
            entry.lastAccessTime = System.currentTimeMillis();
            if (isWrite) {
                entry.modified = true;
            }

            int frame = entry.frameNumber;
            if (frame < 0) {
                throw new RuntimeException("Página " + pagina + " do processo " + pcb.pid + " sem frame associado");
            }
            int endFisico = frame * tamPg + offset;

            if (globalTrace || pcb.trace) {
                System.out.println("Tradução: endLog=" + endLogico + " → pg=" + pagina +
                        ", offset=" + offset + ", frame=" + frame + " → endFis=" + endFisico);
            }

            return endFisico;
        } finally {
            lock.unlock();
        }
    }

    public void logStateChange(PCB pcb, String reason, PCB.ProcState fromState, PCB.ProcState toState) {
        if (stateLogger != null) {
            stateLogger.log(pcb, reason, fromState, toState);
        }
    }

    public StateLogger getStateLogger() {
        return stateLogger;
    }
    
    /**
     * Notificação utilizada pelo dispositivo de disco quando uma página é carregada.
     * Responsável por liberar o frame reservado durante o tratamento do page fault.
     */
    public void onPageLoaded(PCB pcb, int pageNumber, int frameNumber) {
        lock.lock();
        try {
            memoryManager.unlockFrame(frameNumber);
            PageTableEntry entry = pcb.pageTable[pageNumber];
            if (entry != null) {
                entry.modified = false;
                entry.lastAccessTime = System.currentTimeMillis();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Tratamento de page fault - ASSÍNCRONO com bloqueio de processo
     */
    private void handlePageFault(PCB pcb, int pageNumber) {
        lock.lock();
        try {
            PageTableEntry entry = pcb.pageTable[pageNumber];
            if (entry.valid) {
                // Outro thread já carregou a página enquanto tratávamos
                return;
            }

            System.out.println("[PAGE_FAULT] Tratando page fault para processo " + pcb.pid + ", página " + pageNumber);

            int frame = memoryManager.allocateFrame(pcb, pageNumber);
            if (frame < 0) {
                System.out.println("[PAGE_FAULT] Sem frames livres, selecionando vítima...");
                frame = memoryManager.selectVictim();
                if (frame < 0) {
                    throw new RuntimeException("ERRO: Não há frames disponíveis para tratamento de page fault");
                }

                MemoryManager.FrameInfo info = memoryManager.getFrameOwner(frame);
                if (info == null || info.owner == null) {
                    throw new RuntimeException("ERRO: Frame " + frame + " não possui dono registrado");
                }

                PCB victimPCB = info.owner;
                int victimPage = info.pageNumber;
                PageTableEntry victimEntry = victimPCB.pageTable[victimPage];

                System.out.println("[PAGE_FAULT] Vitimando página " + victimPage + " do processo " +
                        victimPCB.pid + " (frame " + frame + ")");

                // Marca a página como inválida imediatamente para evitar novos acessos
                victimEntry.valid = false;
                victimEntry.frameNumber = -1;

                memoryManager.lockFrame(frame);

                DiskDevice.DiskOperation saveOperation = new DiskDevice.DiskOperation(
                        DiskDevice.DiskOpType.SAVE_PAGE,
                        victimPCB,
                        victimPage,
                        frame,
                        victimEntry.diskAddress
                );
                diskDevice.addOperation(saveOperation);
            } else {
                memoryManager.lockFrame(frame);
            }

            // Reserva o frame durante o carregamento (evita vitimação reentrante)
            // Reserva o frame (se veio da lista livre, garante metadados; se veio de vitimação, reatribui)
            memoryManager.assignFrame(frame, pcb, pageNumber);

            // Solicita ao disco o carregamento da página
            DiskDevice.DiskOperation loadOperation = new DiskDevice.DiskOperation(
                    DiskDevice.DiskOpType.LOAD_PAGE,
                    pcb,
                    pageNumber,
                    frame,
                    entry.diskAddress
            );
            diskDevice.addOperation(loadOperation);

            // Garante que o processo ficará bloqueado até o fim da operação de disco
            PCB runningNow = scheduler.getRunning();
            if (runningNow == pcb) {
                System.out.println("[PAGE_FAULT] Bloqueando processo " + pcb.pid + " até carga completar");
                scheduler.blockRunningProcess("page_fault");
            } else if (pcb.state != PCB.ProcState.BLOCKED) {
                PCB.ProcState fromState = pcb.state;
                pcb.state = PCB.ProcState.BLOCKED;
                logStateChange(pcb, "page_fault", fromState, PCB.ProcState.BLOCKED);
            }
        } finally {
            lock.unlock();
        }
        // CPU continuará com outro processo enquanto disco carrega a página
    }

    // Carregamento de programa - agora carrega apenas primeira página
    public void carregaPrograma(Program programa, PCB pcb) {
        // Carregar apenas primeira página (lazy loading)
        carregaPagina(pcb, 0, pcb.pageTable[0].frameNumber, programa);
        
        System.out.println("Programa '" + programa.name + "' - primeira página carregada para processo " + pcb.pid);
        System.out.println("  Demais páginas serão carregadas sob demanda");
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
            scheduler.addToReady(pcb, "creation");

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

            PCB.ProcState from = pcb.state;
            pcb.state = PCB.ProcState.TERMINATED;
            logStateChange(pcb, "manual_remove", from, PCB.ProcState.TERMINATED);

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

            switch (pcb.state) {
                case TERMINATED:
                    System.out.println("Processo " + pid + " já foi finalizado");
                    return;
                case BLOCKED:
                    System.out.println("Processo " + pid + " está bloqueado aguardando IO/Disk");
                    return;
                case RUNNING:
                    System.out.println("Processo " + pid + " já está em execução");
                    return;
                default:
                    // NEW ou READY - garantir que está na fila de prontos
                    scheduler.addToReady(pcb, "manual_exec");
                    scheduler.wakeUp();
                    System.out.println("Processo " + pid + " sinalizado para execução pelo escalonador");
            }
        } finally {
            lock.unlock();
        }
    }

    public void execAll() {
        System.out.println("Escalonador em execução automática. Aguardando processos finalizarem...");
        scheduler.setAutoSchedule(true);
        scheduler.wakeUp();

        boolean finished = false;
        while (!finished) {
            lock.lock();
            try {
                finished = processTable.isEmpty();
            } finally {
                lock.unlock();
            }

            if (!finished) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("Nenhum processo restante no sistema");
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
