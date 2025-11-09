package hardware;

import java.util.Arrays;
import java.util.concurrent.*;
import software.PCB;

/**
 * - Mantém slots (cada slot guarda uma página inteira = array de Word)
 * - Permite page-in (read) e page-out (write) de forma assíncrona
 * - Notifica um DiskListener quando a operação termina
 *
 * Observação: Word é a classe usada no projeto para conteúdo de memória.
 */
public class Disk {

    public interface DiskListener {
        // pageInComplete: slot contém os dados lidos do "disco"
        void pageInComplete(PCB process, int pageNumber, int frameIndex, int diskSlot);

        // pageOutComplete: finalizou escrita da página "victim" no disco
        void pageOutComplete(int victimPid, int victimPageNumber, int diskSlot);
    }

    private final Word[][] slots;    // slots do disco (cada slot guarda uma página inteira)
    private final boolean[] used;
    private final int pageSize;
    private final ExecutorService executor;
    private final DiskListener listener;
    private final long ioLatencyMillis; // tempo simulado de IO

    public Disk(int numSlots, int pageSize, DiskListener listener, long ioLatencyMillis) {
        this.pageSize = pageSize;
        this.slots = new Word[numSlots][];
        this.used = new boolean[numSlots];
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor();
        this.ioLatencyMillis = ioLatencyMillis;
    }

    /**
     * Cria uma página "vazia" (preenchida com palavras do tipo DATA).
     *
     * @return um array de Word representando uma página sem conteúdo.
     */
    private Word[] emptyPage() {
        Word[] page = new Word[pageSize];
        for (int i = 0; i < pageSize; i++) {
            page[i] = new Word(Opcode.DATA, -1, -1, -1);
        }
        return page;
    }


    /**
     * Aloca o primeiro slot livre do disco.
     *
     * @return índice do slot alocado, ou -1 se não houver espaço.
     */
    public synchronized int allocateSlot() {
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                used[i] = true;
                if (slots[i] == null)
                    slots[i] = emptyPage();
                return i;
            }
        }
        return -1; // sem espaço
    }


    /**
     * Libera um slot previamente alocado.
     *
     * @param idx Índice do slot a ser liberado.
     */
    public synchronized void freeSlot(int idx) {
        if (idx >= 0 && idx < used.length) {
            used[idx] = false;
            slots[idx] = null;
        }
    }

    /**
     * Retorna uma cópia dos dados armazenados em um slot (uso síncrono).
     *
     * @param idx Índice do slot.
     * @return cópia da página armazenada, ou {@code null} se inválido.
     */
    public synchronized Word[] readSlotSync(int idx) {
        if (idx < 0 || idx >= slots.length || !used[idx]) return null;
        return slots[idx].clone();
    }

    /**
     * Grava uma página em um slot específico (uso síncrono).
     *
     * @param idx  Índice do slot a ser gravado.
     * @param data Página a ser escrita.
     */
    public synchronized void writeSlotSync(int idx, Word[] data) {
        if (idx < 0 || idx >= slots.length) return;
        slots[idx] = data.clone();
        used[idx] = true;
    }

    /**
     * Solicita leitura de uma página do disco (Page-In).
     * A operação é executada de forma assíncrona e notifica o listener
     * quando a leitura é concluída.
     *
     * @param process    Processo que solicitou a leitura.
     * @param pageNumber Número da página.
     * @param frameIndex Frame de destino na memória.
     * @param diskSlot   Slot de origem no disco (-1 indica primeira carga).
     */
    public void requestPageIn(final PCB process, final int pageNumber, final int frameIndex, final int diskSlot) {
        executor.submit(() -> {
            try {
                Thread.sleep(ioLatencyMillis); // simula latência do disco
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Word[] data;
            if (diskSlot >= 0) {
                synchronized (this) {
                    data = slots[diskSlot] == null ? new Word[pageSize] : slots[diskSlot].clone();
                }
            } else {
                data = process.getProgramPage(pageNumber);
            }
            // Notifica o listener - passa diskSlot para referência
            if (listener != null) {
                listener.pageInComplete(process, pageNumber, frameIndex, diskSlot);
            }
        });
    }

    /**
     * Solicita gravação de uma página em disco (Page-Out).
     * A operação é executada de forma assíncrona e notifica o listener ao concluir.
     *
     * @param victimPid        PID do processo dono da página.
     * @param victimPageNumber Número da página a ser salva.
     * @param pageData         Dados da página.
     * @param diskSlot         Slot onde gravar (-1 para alocar novo).
     */
    public void requestPageOut(final int victimPid, final int victimPageNumber, final Word[] pageData, final int diskSlot) {
        executor.submit(() -> {
            try {
                Thread.sleep(ioLatencyMillis); // simula o tempo de escrita no disco
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int slot = diskSlot;
            synchronized (this) {
                if (slot < 0) {
                    slot = allocateSlot();
                }

                if (slot >= 0) {
                    // Tem espaço no disco: grava a página
                    slots[slot] = pageData.clone();
                    used[slot] = true;
                } else {
                    System.out.println("[DISK] ERRO: sem espaço para gravar página " + victimPageNumber +
                            " do processo " + victimPid + ". Operação de Page-Out abortada.");

                    if (listener != null) {
                        listener.pageOutComplete(victimPid, victimPageNumber, -1);
                    }
                    return;
                }
            }

            // Ok para o callback (SO) que a gravação foi concluída com sucesso
            if (listener != null) {
                listener.pageOutComplete(victimPid, victimPageNumber, slot);
            }
        });
    }



    /**
     * Finaliza o executor responsável pelas operações de I/O.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
}

