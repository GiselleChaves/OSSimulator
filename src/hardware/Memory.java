package hardware;

public class Memory {
    public Word[] pos; // pos[i] é a posição i da memória. cada posição é uma palavra.
    private int tamMem;
    private int tamPg;
    private int nroFrames;

    public Memory(int tamMem, int tamPg) {
        this.tamMem = tamMem;
        this.tamPg = tamPg;
        this.nroFrames = tamMem / tamPg;
        pos = new Word[tamMem];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = new Word(Opcode.___, -1, -1, -1);
        }
    }

    public Memory(int size) {
        this(size, 8); // default tamPg = 8
    }

    public int getTamMem() { return tamMem; }
    public int getTamPg() { return tamPg; }
    public int getNroFrames() { return nroFrames; }

    public Word read(int enderecoFisico) {
        if (enderecoFisico < 0 || enderecoFisico >= tamMem) {
            throw new RuntimeException("Acesso inválido à memória: " + enderecoFisico);
        }
        return pos[enderecoFisico];
    }

    public void write(int enderecoFisico, Word w) {
        if (enderecoFisico < 0 || enderecoFisico >= tamMem) {
            throw new RuntimeException("Acesso inválido à memória: " + enderecoFisico);
        }
        pos[enderecoFisico] = w;
    }
}

