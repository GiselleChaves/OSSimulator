package software;

import hardware.Interrupts;
import hardware.DiskDevice;

// ------- I N T E R R U P C O E S - rotinas de tratamento ------
public class InterruptHandling {
	private SO so; // referencia ao SO

	public InterruptHandling(SO so) {
		this.so = so;
	}

	public void handle(Interrupts irpt) {
		switch (irpt) {
			case intTimer:
				handleTimer();
				break;
			case intSysCallStop:
				handleSysCallStop();
				break;
			case intEnderecoInvalido:
				handleMemoryViolation();
				break;
			case intOverflow:
				handleOverflow();
				break;
			case intInstrucaoInvalida:
				handleInvalidInstruction();
				break;
			case intIO:
				// IO é tratado diretamente via handleIO(PCB)
				break;
			case intPageFault:
				handlePageFault();
				break;
			case intDiskIO:
				// DiskIO é tratado diretamente via handleDiskIO(PCB)
				break;
			default:
				System.out.println("Interrupção desconhecida: " + irpt);
				break;
		}
	}
	
	/**
	 * Tratamento de interrupção de IO
	 * Chamado quando dispositivo de IO termina operação
	 */
	public void handleIO(PCB process) {
		String phase = process.terminating ? "auto_out" : "io";
		System.out.println(String.format("[INT_IO] conclusão %s -> pid=%d (%s)",
				phase, process.pid, process.nome));
		process.ioCompleted = true;

		if (process.terminating) {
			so.completeTerminationAfterIO(process);
			return;
		}

		so.scheduler.unblockProcess(process, "io");
	}
	
	/**
	 * Tratamento de interrupção de Disco
	 * Chamado quando dispositivo de Disco termina operação de paginação
	 */
	public void handleDisk(DiskDevice.DiskOperation operation) {
		System.out.println(String.format("[INT_DISK] conclusão %s -> pid=%d (%s) pg=%d",
				operation.type, operation.process.pid, operation.process.nome, operation.pageNumber));
		
		// Se foi uma operação de LOAD_PAGE, o processo pode ser desbloqueado
		if (operation.type == DiskDevice.DiskOpType.LOAD_PAGE) {
			PCB process = operation.process;
			so.scheduler.unblockProcess(process, "page");
		}
		
		if (operation.type == DiskDevice.DiskOpType.SAVE_PAGE) {
			System.out.println("[INT_DISK] SAVE_PAGE concluído para pid " 
				+ operation.process.pid + ", pg " + operation.pageNumber 
				+ " (frame " + operation.frameNumber + ")");
			// sem unblock aqui; o desbloqueio ocorre ao término do LOAD_PAGE
		}
	}
	
	/**
	 * Tratamento de page fault
	 */
	private void handlePageFault() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("[PAGE_FAULT] Processo " + running.pid + " sinalizou page fault (tratamento assíncrono em andamento)");
		}
	}
	
	private void handleTimer() {
		// logs de contexto ficam no Scheduler.onTimer()
		so.scheduler.onTimer();
	}
	
	private PCB resolveRunningOrCurrent() {
		PCB running = so.scheduler.getRunning();
		if (running == null && so.hw.cpu.getCurrentPCB() != null) {
			running = so.hw.cpu.getCurrentPCB();
		}
		return running;
	}
	
	private void handleSysCallStop() {
		so.terminateRunning("int_syscall_stop");
	}
	
	private void handleMemoryViolation() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("MEMORY_VIOLATION: Processo " + running.pid + " tentou acessar endereço inválido");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid, "memory_violation");
			so.scheduler.scheduleNext();
		}
	}
	
	private void handleOverflow() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("OVERFLOW: Processo " + running.pid + " causou overflow aritmético");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid, "overflow");
			so.scheduler.scheduleNext();
		}
	}
	
	private void handleInvalidInstruction() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("INVALID_INSTRUCTION: Processo " + running.pid + " tentou executar instrução inválida");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid, "invalid_instruction");
			so.scheduler.scheduleNext();
		}
	}
}
