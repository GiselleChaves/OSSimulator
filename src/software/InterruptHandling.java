package software;

import hardware.Interrupts;

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
			default:
				System.out.println("Interrupção desconhecida: " + irpt);
				break;
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
