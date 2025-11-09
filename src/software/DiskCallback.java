package software;

import hardware.Disk;
import hardware.Word;

/**
 * Implementa o listener do disco.
 * É notificada quando operações assíncronas de Page-In ou Page-Out terminam.
 */
public class DiskCallback implements Disk.DiskListener {

    private final SO so;

    public DiskCallback(SO so) {
        this.so = so;
    }

    @Override
    public void pageInComplete(PCB process, int pageNumber, int frameIndex, int diskSlot) {
        System.out.printf(
                "DISK: page-in concluído (pid=%d, pg=%d, frame=%d, slot=%d)\n",
                process.pid, pageNumber, frameIndex, diskSlot
        );

        // Recupera a página do programa
        Word[] data = process.getProgramPage(pageNumber);

        // Copia para a memória física
        int physStart = frameIndex * so.hw.mem.getTamPg();
        for (int i = 0; i < so.hw.mem.getTamPg(); i++) {
            so.hw.mem.write(physStart + i, data[i]);
        }

        // Atualiza tabela de páginas
        process.pageTable[pageNumber] = frameIndex;

        // Desbloqueia o processo
        process.state = PCB.ProcState.READY;
        so.scheduler.addToReady(process);

        so.getBlockedProcesses().remove(process.pid);
    }

    /**
     * Callback acionado pelo {@link hardware.Disk} ao término de uma operação de Page-Out.
     *
     * @param victimPid          identificador (PID) do processo cuja página foi gravada.
     * @param victimPageNumber   número da página virtual que foi enviada para o disco.
     * @param diskSlot           índice do slot de disco utilizado; -1 se não houver espaço.
     */
    @Override
    public void pageOutComplete(int victimPid, int victimPageNumber, int diskSlot) {
        if (diskSlot == -1) {
            System.out.println("[SO] ERRO: Disco cheio! Falha ao salvar página " + victimPageNumber +
                    " do processo " + victimPid);

            PCB victim = so.getPCB(victimPid);
            if (victim != null) {
                victim.state = PCB.ProcState.TERMINATED;
                so.rm(victimPid); // remove o processo e libera memória
            }

            so.scheduler.scheduleNext(); // avança o escalonador
            return;
        }

        System.out.println("[DISK] Page-Out concluído para processo " + victimPid +
                ", página " + victimPageNumber + ", slot " + diskSlot);
    }
}
