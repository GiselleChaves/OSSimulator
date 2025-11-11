package menagers;

import hardware.Opcode;
import hardware.Word;
import software.PCB;

import java.util.*;

public class MemoryManager {
    private int memSize;        // tamanho total da memória em palavras
    private int pgSize;         // tamanho da página / frame em palavras
    private int frameQuantity;     // número total de frames = memSize / pgSize
    private boolean[] frames;  // vetor que indica se o frame i está ocupado (true) ou livre (false)
    private boolean[] frameLocked; // indica frames reservados/indisponíveis (ex. page fault em andamento)
    private Word[] pos;
    
    // Para política de substituição de páginas
    private Map<Integer, FrameInfo> frameOwners; // frame -> informações sobre dono
    
    public static class FrameInfo {
        public PCB owner;
        public int pageNumber;
        
        public FrameInfo(PCB owner, int pageNumber) {
            this.owner = owner;
            this.pageNumber = pageNumber;
        }
    }

    public MemoryManager(int memSize, int pgSize) {
        this.memSize = memSize;
        this.pgSize = pgSize;
        this.frameQuantity = memSize / pgSize;
        this.frames = new boolean[frameQuantity];
        this.frameLocked = new boolean[frameQuantity];
        this.frameOwners = new HashMap<>();
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
            if(!frames[i] && !frameLocked[i]) {
                frames[i] = true;
                frameLocked[i] = false;
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
                frameLocked[frameIndex] = false;
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
    
    /**
     * Aloca um único frame para uma página específica de um processo
     */
    public int allocateFrame(PCB owner, int pageNumber) {
        // Buscar frame livre
        for (int i = 0; i < frameQuantity; i++) {
            if (!frames[i]) {
                frames[i] = true;
                frameOwners.put(i, new FrameInfo(owner, pageNumber));
                return i;
            }
        }
        // Não há frame livre
        return -1;
    }
    
    /**
     * Desaloca um frame específico
     */
    public void deallocateFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frames.length) {
            frames[frameIndex] = false;
            frameLocked[frameIndex] = false;
            frameOwners.remove(frameIndex);
        }
    }
    
    /**
     * Escolhe uma vítima usando política FIFO simplificada
     * Retorna o frame a ser liberado
     */
    public int selectVictim() {
        // Política simples: primeiro frame ocupado (FIFO)
        for (int i = 0; i < frameQuantity; i++) {
            if (frames[i] && !frameLocked[i]) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Obtém informações sobre o dono de um frame
     */
    public FrameInfo getFrameOwner(int frameIndex) {
        return frameOwners.get(frameIndex);
    }
    
    /**
     * Atribui (ou reatribui) explicitamente o dono de um frame.
     * Utilizado durante o tratamento de page-fault para reservar um frame
     * a uma página que será carregada pelo disco.
     */
    public void assignFrame(int frameIndex, PCB owner, int pageNumber) {
        if (frameIndex < 0 || frameIndex >= frames.length) {
            throw new IllegalArgumentException("Frame inválido: " + frameIndex);
        }
        frames[frameIndex] = true;
        frameOwners.put(frameIndex, new FrameInfo(owner, pageNumber));
    }
    
    /**
     * Marca um frame como reservado (não pode ser escolhido como vítima enquanto true).
     */
    public void lockFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frameLocked.length) {
            frameLocked[frameIndex] = true;
        }
    }
    
    /**
     * Libera a reserva de um frame previamente marcado com lockFrame.
     */
    public void unlockFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frameLocked.length) {
            frameLocked[frameIndex] = false;
        }
    }
    
    /**
     * Verifica se há frames livres
     */
    public boolean hasFreeFrame() {
        for (boolean frame : frames) {
            if (!frame) return true;
        }
        return false;
    }
    
    /**
     * Conta frames livres
     */
    public int countFreeFrames() {
        int count = 0;
        for (boolean frame : frames) {
            if (!frame) count++;
        }
        return count;
    }
}
