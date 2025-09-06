package menagers;

import hardware.Opcode;
import hardware.Word;

public class MemoryManager {
    private int memSize;        // tamanho total da memória em palavras
    private int pgSize;         // tamanho da página / frame em palavras
    private int frameQuantity;     // número total de frames = memSize / pgSize
    private boolean[] frames;  // vetor que indica se o frame i está ocupado (true) ou livre (false)
    private Word[] pos;

    public MemoryManager(int memSize, int pgSize) {
        this.memSize = memSize;
        this.pgSize = pgSize;
        this.frameQuantity = memSize / pgSize;
        this.frames = new boolean[frameQuantity];
        pos = new Word[memSize];
        for (int i = 0; i < memSize; i++) {
            pos[i] = new Word(Opcode.DATA, 0, 0, 0);
        }
    }

    /*
    * Allocate frames
    */
    public int[] allocate(int wordSize) {
        int pgNumber = (int) Math.ceil((double) wordSize / getPgSize());
        int[] pgTable = new int[pgNumber];
        int count = 0;

        // Buscar frames livres
        for(int i = 0; i < frameQuantity && count < pgNumber; i++) {
            if(!frames[i]) {
                frames[i] = true;
                pgTable[count++] = i;
            }
        }
        
        // Se não conseguiu alocar todos os frames necessários
        if(count < pgNumber) {
            // Desfazer alocações parciais
            for(int i = 0; i < count; i++) {
                frames[pgTable[i]] = false;
            }
            return null;
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

    /**
     * Escreve uma palavra na memória no endereço especificado
     * @param endereco posição na memória
     * @param valor palavra a ser escrita
     * @return true se a operação foi bem-sucedida, false caso endereço inválido
     */
    public boolean write(int endereco, int valor) {
        if (endereco < 0 || endereco >= pos.length) {
            System.out.println("Erro: endereço inválido na memória: " + endereco);
            return false;
        }
        pos[endereco] = new Word(Opcode.DATA, 0, 0, valor);
        return true;
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

    public boolean[] getFrames() {
        return frames;
    }
}
