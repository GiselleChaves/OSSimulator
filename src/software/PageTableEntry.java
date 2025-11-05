package software;

/**
 * Entrada da tabela de páginas com flags para memória virtual
 */
public class PageTableEntry {
    public int frameNumber;      // Número do frame físico (-1 se não estiver em memória)
    public boolean valid;         // true se página está em memória física
    public boolean modified;      // true se página foi modificada (dirty bit)
    public int diskAddress;       // Endereço no disco onde página está salva (-1 se nunca foi vitimada)
    public long lastAccessTime;   // Para política de substituição (LRU)
    
    public PageTableEntry() {
        this.frameNumber = -1;
        this.valid = false;
        this.modified = false;
        this.diskAddress = -1;
        this.lastAccessTime = 0;
    }
    
    public PageTableEntry(int frameNumber) {
        this.frameNumber = frameNumber;
        this.valid = true;
        this.modified = false;
        this.diskAddress = -1;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        if (valid) {
            return String.format("frame=%d, valid=true, modified=%b", frameNumber, modified);
        } else if (diskAddress >= 0) {
            return String.format("disk=%d, valid=false", diskAddress);
        } else {
            return "not_loaded";
        }
    }
}

