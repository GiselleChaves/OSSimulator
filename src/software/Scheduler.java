package software;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Scheduler implements Runnable {
    private BlockingQueue<PCB> readyQueue;
    private BlockingQueue<PCB> blockedQueue; // Fila de processos bloqueados
    private PCB running;
    private SO so;
    
    private ReentrantLock lock;
    private Condition hasWork;
    private boolean active;
    private boolean autoSchedule; // Controla se deve escalonar automaticamente
    
    public Scheduler(SO so) {
        this.so = so;
        this.readyQueue = new LinkedBlockingQueue<>();
        this.blockedQueue = new LinkedBlockingQueue<>();
        this.running = null;
        this.lock = new ReentrantLock();
        this.hasWork = lock.newCondition();
        this.active = true;
        this.autoSchedule = true; // Escalonamento automático habilitado por padrão
    }
    
    private boolean isRunningProcess(PCB pcb) {
        return running != null && running.pid == pcb.pid;
    }

    private boolean isInReadyQueue(PCB pcb) {
        return readyQueue.contains(pcb);
    }

    private void enqueueReadyIfNeeded(PCB pcb) {
        if (!isInReadyQueue(pcb) && !isRunningProcess(pcb)) {
            readyQueue.offer(pcb);
        }
    }

    private void moveProcessToReady(PCB pcb, String reason) {
        if (pcb == null || pcb.state == PCB.ProcState.TERMINATED) {
            return;
        }
        PCB.ProcState from = pcb.state;
        boolean alreadyQueued = isInReadyQueue(pcb);
        enqueueReadyIfNeeded(pcb);
        pcb.state = PCB.ProcState.READY;
        if (!alreadyQueued) {
            String label = reason != null ? reason : "-";
            System.out.println(String.format("[READY] pid=%d (%s) <- %s",
                    pcb.pid, pcb.nome, label));
        }
        so.logStateChange(pcb, reason, from, PCB.ProcState.READY);
    }

    public void addToReady(PCB pcb, String reason) {
        lock.lock();
        try {
            moveProcessToReady(pcb, reason);
            hasWork.signal();
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
                moveProcessToReady(running, "timer");
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
            scheduleNextLocked();
        } finally {
            lock.unlock();
        }
    }
    
    private void scheduleNextLocked() {
        if (running == null && !readyQueue.isEmpty()) {
            PCB next = readyQueue.poll();
            if (next != null) {
                PCB.ProcState from = next.state;
                running = next;
                running.state = PCB.ProcState.RUNNING;
                so.hw.cpu.setContext(running);
                so.logStateChange(running, "dispatch", from, PCB.ProcState.RUNNING);
                System.out.println(String.format("[CTX] Switch -> pid=%d (%s) pc=%d", next.pid, running.nome, running.pc));
            }
        }
    }
    
    /**
     * Bloqueia o processo em execução (para aguardar IO)
     */
    public void blockRunningProcess(String reason) {
        lock.lock();
        try {
            if (running != null) {
                String label = reason != null ? reason : "-";
                System.out.println(String.format("[BLOCK] pid=%d (%s) -> %s",
                        running.pid, running.nome, label));
                so.hw.cpu.saveContext(running);

                running.blockReasons.add(reason);
                if (running.state != PCB.ProcState.BLOCKED) {
                    PCB.ProcState from = running.state;
                    running.state = PCB.ProcState.BLOCKED;
                    so.logStateChange(running, reason, from, PCB.ProcState.BLOCKED);
                    blockedQueue.offer(running);
                }
                running = null;
                
                // Escalona próximo processo
                if (autoSchedule) {
                    hasWork.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Desbloqueia processo e coloca de volta na fila READY
     */
    public void unblockProcess(PCB pcb, String reason) {
        lock.lock();
        try {
            pcb.blockReasons.remove(reason);

            if (pcb.blockReasons.isEmpty() && pcb.state == PCB.ProcState.BLOCKED) {
                blockedQueue.remove(pcb);
                moveProcessToReady(pcb, "unblock:" + reason);
                hasWork.signal();
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
            
            // Remove da fila BLOCKED
            blockedQueue.removeIf(pcb -> pcb.pid == pid);
            
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
        return !readyQueue.isEmpty() || running != null || !blockedQueue.isEmpty();
    }
    
    public int getBlockedCount() {
        return blockedQueue.size();
    }
    
    public void setAutoSchedule(boolean autoSchedule) {
        lock.lock();
        try {
            this.autoSchedule = autoSchedule;
            if (autoSchedule && (running == null) && !readyQueue.isEmpty()) {
                hasWork.signal();
            } else if (autoSchedule) {
                hasWork.signalAll();
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
                while (active && (!autoSchedule || running != null || readyQueue.isEmpty())) {
                    hasWork.await();
                }
                
                if (!active) break;
                
                scheduleNextLocked();
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
    
    public void wakeUp() {
        lock.lock();
        try {
            hasWork.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
