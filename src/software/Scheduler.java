package software;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Scheduler implements Runnable {
    private BlockingQueue<PCB> readyQueue;
    private PCB running;
    private SO so;
    
    private ReentrantLock lock;
    private Condition hasWork;
    private boolean active;
    private boolean autoSchedule; // Controla se deve escalonar automaticamente
    
    public Scheduler(SO so) {
        this.so = so;
        this.readyQueue = new LinkedBlockingQueue<>();
        this.running = null;
        this.lock = new ReentrantLock();
        this.hasWork = lock.newCondition();
        this.active = true;
        this.autoSchedule = false; // Por padrão, não executa automaticamente
    }
    
    public void addToReady(PCB pcb) {
        lock.lock();
        try {
            pcb.state = PCB.ProcState.READY;
            readyQueue.offer(pcb);
            System.out.println("Processo " + pcb.pid + " (" + pcb.nome + ") adicionado à fila READY");
            
            // Só sinaliza se autoSchedule estiver ativo
            if (autoSchedule) {
                hasWork.signal();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void onTimer() {
        lock.lock();
        try {
            if (running != null) {
                int prevPid = running.pid;
                int prevPc = running.pc;
                int delta = so.hw.cpu.getDelta();
                System.out.println(String.format("[CTX] TIMER: preempção após %d instruções | from pid=%d pc=%d", delta, prevPid, prevPc));
                // Salvar contexto
                so.hw.cpu.saveContext(running);
                // Colocar de volta na fila READY
                running.state = PCB.ProcState.READY;
                readyQueue.offer(running);
                running = null;
                hasWork.signal(); // Sinaliza para escalonar próximo
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void scheduleNext() {
        lock.lock();
        try {
            if (running == null && !readyQueue.isEmpty()) {
                PCB next = readyQueue.poll();
                if (next != null) {
                    int nextPid = next.pid;
                    running = next;
                    running.state = PCB.ProcState.RUNNING;
                    so.hw.cpu.setContext(running);
                    System.out.println(String.format("[CTX] Switch -> pid=%d (%s) pc=%d", nextPid, running.nome, running.pc));
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void removeProcess(int pid) {
        lock.lock();
        try {
            // Remove da fila READY
            readyQueue.removeIf(pcb -> pcb.pid == pid);
            
            // Se está em execução, remove
            if (running != null && running.pid == pid) {
                running = null;
                if (autoSchedule) {
                    hasWork.signal(); // Sinaliza para escalonar próximo
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public PCB getRunning() {
        lock.lock();
        try {
            return running;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean hasReadyProcesses() {
        return !readyQueue.isEmpty() || running != null;
    }
    
    public void setAutoSchedule(boolean autoSchedule) {
        lock.lock();
        try {
            this.autoSchedule = autoSchedule;
            if (autoSchedule && hasReadyProcesses()) {
                hasWork.signal();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isAutoSchedule() {
        return autoSchedule;
    }
    
    @Override
    public void run() {
        while (active) {
            lock.lock();
            try {
                // Só aguarda sinal se autoSchedule estiver ativo
                if (autoSchedule) {
                    while (readyQueue.isEmpty() && active) {
                        hasWork.await();
                    }
                    
                    if (!active) break;
                    
                    // Escalona próximo processo se não há nenhum rodando
                    scheduleNext();
                } else {
                    // Se não está em autoSchedule, apenas aguarda um pouco
                    hasWork.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }
    }
    
    public void shutdown() {
        lock.lock();
        try {
            active = false;
            hasWork.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
