package software;

import java.util.HashSet;
import java.util.Set;

public class PCB {
    public PCB(int pid, int inicio, int fim, String programName) {
        this.pid = pid;
        this.inicio = inicio;
        this.fim = fim;
        this.programName = programName;
    }

    public enum ProcState {
        NEW, READY, RUNNING, BLOCKED, TERMINATED
    }

    // Identificação do processo
    public int pid;
    public String nome;
    
    // Contexto da CPU
    public int pc;
    public int[] reg;
    public boolean trace;
    
    // Gerenciamento de memória
    public PageTableEntry[] pageTable;
    public int numPages;
    public int tamanhoEmPalavras;

    public int inicio;
    public int fim;
    public String programName;

    public boolean ioPending = false;
    public boolean ioCompleted = false;
    public int ioTypeCode = 0;
    public int ioLogicalAddr = -1;

    public Set<String> blockReasons = new HashSet<>();
    
    // Estado do processo
    public ProcState state;

    public PCB(int pid, String nome, int tamanhoEmPalavras, int tamPg) {
        this.pid = pid;
        this.nome = nome;
        this.tamanhoEmPalavras = tamanhoEmPalavras;
        this.pc = 0;
        this.reg = new int[10];
        this.trace = false;
        this.state = ProcState.NEW;
        
        // Calcular número de páginas necessárias
        this.numPages = (int) Math.ceil((double) tamanhoEmPalavras / tamPg);
        this.pageTable = new PageTableEntry[numPages];
        
        // Inicializar entradas da tabela de páginas
        for (int i = 0; i < numPages; i++) {
            this.pageTable[i] = new PageTableEntry();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("PCB[pid=%d, nome=%s, state=%s, pc=%d]\n", pid, nome, state, pc));
        sb.append("Registradores: ");
        for (int i = 0; i < reg.length; i++) {
            sb.append(String.format("r%d=%d ", i, reg[i]));
        }
        sb.append("\n");
        sb.append(String.format("Memória: %d palavras, %d páginas\n", tamanhoEmPalavras, numPages));
        sb.append("Tabela de páginas:\n");
        for (int i = 0; i < pageTable.length; i++) {
            sb.append(String.format("  pg%d: %s\n", i, pageTable[i].toString()));
        }
        return sb.toString();
    }
} 