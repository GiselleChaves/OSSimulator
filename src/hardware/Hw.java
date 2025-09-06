package hardware;

public class Hw {
    public Memory mem;
    public CPU cpu;
    
    private int tamMem;
    private int tamPg;
    private int delta;

    public Hw(int tamMem) {
        this(tamMem, 8, 5); // defaults: tamPg=8, delta=5
    }
    
    public Hw(int tamMem, int tamPg, int delta) {
        this.tamMem = tamMem;
        this.tamPg = tamPg;
        this.delta = delta;
        
        mem = new Memory(tamMem, tamPg);
        cpu = new CPU(mem, true); // true liga debug
        cpu.setDelta(delta);
    }
    
    public int getTamMem() { return tamMem; }
    public int getTamPg() { return tamPg; }
    public int getDelta() { return delta; }
}
