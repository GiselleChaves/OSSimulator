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
		System.out.println("[INT_IO] Dispositivo de IO terminou operação para processo " + process.pid);
		
		// Processo estava bloqueado esperando IO, agora pode voltar para READY
		if (process.state == PCB.ProcState.BLOCKED) {
			so.scheduler.unblockProcess(process, "io_complete");
			System.out.println("[INT_IO] Processo " + process.pid + " desbloqueado e movido para READY");
		}
	}
	
	/**
	 * Tratamento de interrupção de Disco
	 * Chamado quando dispositivo de Disco termina operação de paginação
	 */
	public void handleDisk(DiskDevice.DiskOperation operation) {
		System.out.println("[INT_DISK] Dispositivo de Disco terminou " + operation.type + 
		                   " para processo " + operation.process.pid + ", página " + operation.pageNumber);
		
		// Se foi uma operação de LOAD_PAGE, o processo pode ser desbloqueado
		if (operation.type == DiskDevice.DiskOpType.LOAD_PAGE) {
			PCB process = operation.process;
			if (process.state == PCB.ProcState.BLOCKED) {
				so.scheduler.unblockProcess(process, "page_loaded");
				System.out.println("[INT_DISK] Processo " + process.pid + " desbloqueado após carga de página");
			}
		}
		// Se foi SAVE_PAGE, nenhuma ação adicional necessária (página já foi invalidada)
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
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("STOP: Processo " + running.pid + " solicitou finalização");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid);
			so.scheduler.scheduleNext();
		}
	}
	
	private void handleMemoryViolation() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("MEMORY_VIOLATION: Processo " + running.pid + " tentou acessar endereço inválido");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid);
			so.scheduler.scheduleNext();
		}
	}
	
	private void handleOverflow() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("OVERFLOW: Processo " + running.pid + " causou overflow aritmético");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid);
			so.scheduler.scheduleNext();
		}
	}
	
	private void handleInvalidInstruction() {
		PCB running = resolveRunningOrCurrent();
		if (running != null) {
			System.out.println("INVALID_INSTRUCTION: Processo " + running.pid + " tentou executar instrução inválida");
			running.state = PCB.ProcState.TERMINATED;
			so.rm(running.pid);
			so.scheduler.scheduleNext();
		}
	}
}
