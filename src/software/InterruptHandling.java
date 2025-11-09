package software;

import hardware.Interrupts;
import hardware.Word;
import menagers.MemoryManager;


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
			case intPageFault:
				handlePageFault();
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

	private void handlePageFault() {
		MemoryManager mm = so.getMemoryManager();
		PCB running = resolveRunningOrCurrent();
		if (running == null) return;

		System.out.println("PAGE FAULT detectado no processo " + running.pid);

		// Descobre qual página causou o fault
		int logicalAddress = so.hw.cpu.getPc();
		int pageNumber = logicalAddress / so.hw.mem.getTamPg();

		// Tenta encontrar um frame livre
		int frameIndex = mm.findFreeFrame();

		// Se nenhum frame livre, precisamos desalocar uma página (Page-Out)
		if (frameIndex < 0) {
			int victimFrame = mm.findVictimFrame();

			// Encontrar o processo dono da página vítima
			PCB victim = so.findProcessByFrame(victimFrame);
			if (victim == null) {
				System.out.println("Erro: nenhum processo dono do frame vítima!");
				return;
			}

			int victimPage = victim.getPageToEvict();

			// Ler os dados da página vítima da memória
			Word[] pageData = so.hw.mem.readFrame(victimFrame);

			// Solicitar ao disco para gravar essa página
			int oldSlot = victim.getDiskSlotForPage(victimPage);
			so.getDisk().requestPageOut(victim.pid, victimPage, pageData, oldSlot);

			System.out.println("Page-Out iniciado para processo " + victim.pid + ", página " + victimPage);
			return; // o callback (DiskCallback) continuará o processo depois
		}

		//Se há frame livre, carregar a página que faltava (Page-In)
		int diskSlot = running.getDiskSlotForPage(pageNumber);
		so.getDisk().requestPageIn(running, pageNumber, frameIndex, diskSlot);

		// Bloqueia o processo enquanto o disco faz o Page-In
		running.state = PCB.ProcState.BLOCKED;
		so.getBlockedProcesses().put(running.pid, running);

		// Força o escalonador a buscar outro processo
		so.scheduler.scheduleNext();
	}



}
