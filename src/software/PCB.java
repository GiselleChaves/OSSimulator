package software;

import hardware.Word;

import java.util.HashMap;
import java.util.Map;

/**
 * PCB - Bloco de Controle de Processo (versão com suporte a obter páginas do programa)
 */
public class PCB {

    public int pid;
    public String nome;
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

    /**
     * Imagem completa do programa (array linear de Word como em Program.image).
     * Usado para "primeira carga" de páginas (quando diskSlot == -1).
     */
    public Word[] programImage;

    /**
     * Tamanho da página, em palavras. Necessário para devolver páginas.
     */
    public int pageSize;

    // Construtor já existente utilizado pelo seu SO (adapte parâmetros conforme seu projeto)
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

        // Inicializar novos campos
        this.programImage = null;
        this.pageSize = tamPg;
    }

    /**
     * Define a imagem do programa (deve ser chamada logo após criar o PCB).
     *
     * @param img     array linear de Word que representa a imagem do programa
     * @param pageSize tamanho da página em palavras (deve coincidir com hw.mem.getTamPg())
     */
    public void setProgramImage(Word[] img, int pageSize) {
        this.programImage = img;
        this.pageSize = pageSize;
        // recalcula numPages caso necessário
        this.numPages = (int) Math.ceil((double) (tamanhoEmPalavras) / pageSize);
        // garante que pageTable existe
        if (this.pageTable == null || this.pageTable.length != this.numPages) {
            this.pageTable = new int[this.numPages];
        }
    }

    /**
     * Retorna uma cópia da página `pageNumber` da imagem do programa.
     * Se a página ultrapassar o fim da imagem, preenche com Word(Opcode.DATA,...).
     *
     * @param pageNumber número da página (0..numPages-1)
     * @return Word[] com tamanho pageSize (nunca retorna null)
     */
    public Word[] getProgramPage(int pageNumber) {
        Word[] page = new Word[this.pageSize];
        // Preenche com DATA por padrão
        for (int i = 0; i < page.length; i++) {
            page[i] = new Word(hardware.Opcode.DATA, -1, -1, -1);
        }
        if (programImage == null) return page;

        int start = pageNumber * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx >= 0 && idx < programImage.length && programImage[idx] != null) {
                page[i] = programImage[idx];
            }
        }
        return page;
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
        if (pageTable != null) {
            for (int i = 0; i < pageTable.length; i++) {
                sb.append(String.format("pg%d→frame%d ", i, pageTable[i]));
            }
        }
        return sb.toString();
    }

    /**
     * Retorna o número da página a ser removida (vítima) para swap-out.
     * Estratégia simples: retorna a primeira página mapeada.
     *
     * @return número da página vítima, ou -1 se não houver páginas.
     */
    public int getPageToEvict() {
        if (pageTable == null || pageTable.length == 0) return -1;

        return 0;
    }

    // Mapeia número da página lógica → slot no disco (-1 se ainda não foi salva)
    public Map<Integer, Integer> diskSlots = new HashMap<>();

    /**
     * Retorna o slot do disco associado a uma página.
     *
     * @param pageNumber número da página lógica
     * @return índice do slot no disco, ou -1 se a página ainda não está armazenada
     */
    public int getDiskSlotForPage(int pageNumber) {
        return diskSlots.getOrDefault(pageNumber, -1);
    }

    /**
     * Associa uma página lógica a um slot de disco.
     *
     * @param pageNumber número da página lógica
     * @param slot índice do slot no disco
     */
    public void setDiskSlotForPage(int pageNumber, int slot) {
        diskSlots.put(pageNumber, slot);
    }



    public enum ProcState {
        NEW, READY, RUNNING, BLOCKED, TERMINATED
    }
}
