package software;

import hardware.Hw;
import hardware.Word;
import menagers.MemoryManager;
import menagers.Program;
import menagers.Programs;
import util.Utilities;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public Hw hw;

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

    // ============== GERENTE DE MEMÓRIA (GM PAGINADO) ==============

    public boolean gmAloca(int nroPalavras, PCB pcb) {
        int numPages = (int) Math.ceil((double) nroPalavras / hw.mem.getTamPg());
        pcb.numPages = numPages;
        pcb.pageTable = memoryManager.allocate(nroPalavras);

        if (pcb.pageTable == null) {
            System.out.println("ERRO: Não foi possível alocar " + numPages + " páginas para o processo " + pcb.pid);
            return false;
        }

        System.out.println("GM: Alocadas " + numPages + " páginas para processo " + pcb.pid +
                " (" + nroPalavras + " palavras)");
        System.out.print("Frames alocados: ");
        for (int i = 0; i < pcb.pageTable.length; i++) {
            System.out.print("pg" + i + "→frame" + pcb.pageTable[i] + " ");
        }
        System.out.println();

        return true;
    }

    public void gmDesaloca(PCB pcb) {
        if (pcb.pageTable != null) {
            memoryManager.deallocate(pcb.pageTable);
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

        int frame = pcb.pageTable[pagina];
        int endFisico = frame * tamPg + offset;

        if (globalTrace || pcb.trace) {
            System.out.println("Tradução: endLog=" + endLogico + " → pg=" + pagina +
                    ", offset=" + offset + ", frame=" + frame + " → endFis=" + endFisico);
        }

        return endFisico;
    }

    // Carregamento de programa por página
    public void carregaPrograma(Program programa, PCB pcb) {
        Word[] programImage = programa.image;
        int tamPg = hw.mem.getTamPg();

        for (int i = 0; i < programImage.length; i++) {
            int endLogico = i;
            int endFisico = traduzEndereco(pcb, endLogico);
            hw.mem.write(endFisico, programImage[i]);
        }

        System.out.println("Programa '" + programa.name + "' carregado para processo " + pcb.pid);
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

            int pid = nextPid.getAndIncrement();
            PCB pcb = new PCB(pid, nomeProg, programa.image.length, hw.mem.getTamPg());

            // Alocar memória
            if (!gmAloca(programa.image.length, pcb)) {
                return -1;
            }

            // Carregar programa
            carregaPrograma(programa, pcb);

            // Adicionar à tabela de processos
            processTable.put(pid, pcb);

            // Colocar na fila READY
            scheduler.addToReady(pcb);

            System.out.println("Processo criado: pid=" + pid + ", nome=" + nomeProg +
                    ", tamanho=" + programa.image.length + " palavras");

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
                int frame = pcb.pageTable[pg];
                int endLogIni = pg * hw.mem.getTamPg();
                int endLogFim = Math.min(endLogIni + hw.mem.getTamPg() - 1, pcb.tamanhoEmPalavras - 1);
                int endFisIni = frame * hw.mem.getTamPg();
                int endFisFim = endFisIni + hw.mem.getTamPg() - 1;

                sb.append(String.format("  Página %d (end.lóg %d-%d) → Frame %d (end.fís %d-%d)\n",
                        pg, endLogIni, endLogFim, frame, endFisIni, endFisFim));
            }

            return sb.toString();
        } finally {
            lock.unlock();
        }
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

            System.out.println("Executando processo " + pid + " em modo debug (sem preempção)");

            // Remover do escalonador se estiver lá
            scheduler.removeProcess(pid);

            // Modo debug: execução sem preempção
            hw.cpu.setContext(pcb);
            pcb.state = PCB.ProcState.RUNNING;

            // Execução passo a passo até terminar ou dar erro
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
        } finally {
            lock.unlock();
        }
    }

    public void execAll() {
        System.out.println("Iniciando execução escalonada de todos os processos...");

        // Sinalizar escalonador que há trabalho
        scheduler.scheduleNext();

        // Aguardar até todos os processos terminarem
        while (scheduler.hasReadyProcesses()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

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
