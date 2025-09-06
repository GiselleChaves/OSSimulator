package software;

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
    public int[] pageTable;
    public int numPages;
    public int tamanhoEmPalavras;

    public int inicio;
    public int fim;
    public String programName;
    
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
        this.pageTable = new int[numPages];
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
        sb.append("Tabela de páginas: ");
        for (int i = 0; i < pageTable.length; i++) {
            sb.append(String.format("pg%d→frame%d ", i, pageTable[i]));
        }
        return sb.toString();
    }
} 