package menagers;

public class MemoryManager {
    private int memSize;        // tamanho total da memória em palavras
    private int pgSize;         // tamanho da página / frame em palavras
    private int frameQuantity;     // número total de frames = memSize / pgSize
    private boolean[] frames;  // vetor que indica se o frame i está ocupado (true) ou livre (false)

    public MemoryManager(int memSize, int pgSize) {
        this.memSize = memSize;
        this.pgSize = pgSize;
        this.frameQuantity = memSize / pgSize;
        this.frames = new boolean[frameQuantity];  // inicializa todos como false (livres)
    }

    /*
    * Allocate frames
    */
    public int[] allocate(int wordSize) {
        int pgNumber = wordSize / getPgSize();
        int[] pgTable = new int[pgNumber];
        int count = 0;

        for(int i = 0; i < frameQuantity && i < pgNumber; i++) {
            if(!frames[i]) {
                frames[i] = true;
                pgTable[count++] = i;
            }
        }
        if(count < pgNumber) {
            for(int i = 0; i < count; i++) {
                frames[pgTable[i]] = false;
                return null;
            }
        }
        return pgTable;
    }

    /*
    * Deallocate frame
    */
    public boolean deallocate(int[] pgTable) {
        if (pgTable == null || pgTable.length == 0) return false;

        for (int i = 0; i < pgTable.length; i++) {
            int frameIndex = pgTable[i];
            if (frameIndex >= 0 && frameIndex < frames.length) {
                frames[frameIndex] = false;
            }
        }
        return true;
    }

    /*
    * Show frame status
    */
    public void showStatus(){
        System.out.println("Frames");
        for(boolean frame:frames) {
            if(frame) {
                System.out.println("1");
            }else {
                System.out.println("0");
            }
        }
    }

    public int getMemSize() {
        return memSize;
    }

    public void setMemSize(int memSize) {
        this.memSize = memSize;
    }

    public int getPgSize() {
        return pgSize;
    }

    public void setPgSize(int pgSize) {
        this.pgSize = pgSize;
    }

    public int getFrameQuantity() {
        return frameQuantity;
    }

}
