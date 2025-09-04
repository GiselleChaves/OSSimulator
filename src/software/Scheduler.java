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
    
    public Scheduler(SO so) {
        this.so = so;
        this.readyQueue = new LinkedBlockingQueue<>();
        this.running = null;
        this.lock = new ReentrantLock();
        this.hasWork = lock.newCondition();
        this.active = true;
    }
    
    public void addToReady(PCB pcb) {
        lock.lock();
        try {
            pcb.state = PCB.ProcState.READY;
            readyQueue.offer(pcb);
            hasWork.signal(); // Sinaliza que há trabalho
            System.out.println("Processo " + pcb.pid + " (" + pcb.nome + ") adicionado à fila READY");
        } finally {
            lock.unlock();
        }
    }
    
    public void onTimer() {
        lock.lock();
        try {
            if (running != null) {
                System.out.println("TIMER: Preempção do processo " + running.pid + " após delta instruções");
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
                running = readyQueue.poll();
                if (running != null) {
                    running.state = PCB.ProcState.RUNNING;
                                         so.hw.cpu.setContext(running);
                    System.out.println("Escalonando processo " + running.pid + " (" + running.nome + ")");
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
                hasWork.signal(); // Sinaliza para escalonar próximo
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
    
    @Override
    public void run() {
        while (active) {
            lock.lock();
            try {
                // Aguarda sinal de que há trabalho
                while (readyQueue.isEmpty() && active) {
                    hasWork.await();
                }
                
                if (!active) break;
                
                // Escalona próximo processo se não há nenhum rodando
                scheduleNext();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
            
            // Pequena pausa para evitar busy-wait
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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