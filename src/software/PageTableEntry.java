package software;

/**
 * Representa uma entrada da Tabela de Páginas de um processo.
 *
 * Cada entrada indica o estado da página: se está presente em memória,
 * em qual quadro (frame) ela se encontra, ou se está armazenada em disco.
 *
 * Contém também bits auxiliares para implementação de políticas
 * de substituição (referenced, dirty).
 *
 * @version 1.0
 */
public class PageTableEntry {

    /** Bit de presença: indica se a página está carregada em memória. */
    public boolean present;

    /** Índice do quadro (frame) físico onde a página está armazenada. */
    public int frameIndex;

    /** Índice do slot no disco onde a página está armazenada (caso tenha sido vitimada). */
    public int diskSlot;

    /** Bit de referência: usado em políticas como LRU ou Second-Chance. */
    public boolean referenced;

    /** Bit de modificação (dirty): indica se a página foi alterada. */
    public boolean dirty;

    /**
     * Construtor padrão: inicializa uma entrada como "não presente".
     */
    public PageTableEntry() {
        this.present = false;
        this.frameIndex = -1;
        this.diskSlot = -1;
        this.referenced = false;
        this.dirty = false;
    }
}

