package menagers;
import menagers.MemoryManager;
import software.PCB;

import java.util.*;

public class ProcessManager {
    private int nextPid = 1;
    private List<PCB> readyQueue = new LinkedList<>();
    private PCB running = null;

    private MemoryManager mm;

    public ProcessManager(MemoryManager mm) {
        this.mm = mm;
    }

    public PCB createProcess(String programName, int[] program) {
        int[] base = mm.allocate(program.length); // retorna posições alocadas
        if (base == null || base.length == 0) {
            System.out.println("Not enough memory to create process.");
            return null;
        }

        // create PCB
        int inicio = base[0];
        int fim = base[base.length - 1];

        //Creating PCB
        PCB pcb = new PCB(nextPid++, inicio, fim, programName);

        // Loading program into memory
        for (int i = 0; i < program.length; i++) {
            mm.write(base[i], program[i]);
        }

        // add to ready queue
        readyQueue.add(pcb);
        System.out.println("Process " + pcb.pid + " created and ready.");
        return pcb;
    }

    public void deallocateProcess(int pid) {
        PCB pcb = find(pid);
        if (pcb == null) {
            System.out.println("Process " + pid + " not found.");
            return;
        }

        mm.deallocate(pcb.inicio, pcb.fim - pcb.inicio + 1);
        readyQueue.remove(pcb);
        if (running == pcb) running = null;

        pcb.state = State.TERMINATED;
        System.out.println("Process " + pid + " removed.");
    }

    public void exec(int pid) {
        PCB pcb = find(pid);
        if (pcb == null) {
            System.out.println("Process " + pid + " not found.");
            return;
        }

        running = pcb;
        pcb.state = State.RUNNING;

        System.out.println("Executing process " + pid);
        // Here you connect your CPU
        // Example: cpu.run(pcb);
    }

    public void listProcesses() {
        System.out.println("Existing processes:");
        for (PCB p : readyQueue) {
            System.out.println("PID: " + p.pid + " | Name: " + p.programName + " | State: " + p.state);
        }
        if (running != null) {
            System.out.println("Running: PID " + running.pid);
        }
    }

    private PCB find(int pid) {
        for (PCB p : readyQueue) if (p.pid == pid) return p;
        if (running != null && running.pid == pid) return running;
        return null;
    }
}

