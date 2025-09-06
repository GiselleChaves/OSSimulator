package hardware;

public class Hw {
    public Memory mem;
    public CPU cpu;

    public Hw(int tamMem) {
        mem = new Memory(tamMem);
        cpu = new CPU(mem, true); // true liga debug
    }
}
