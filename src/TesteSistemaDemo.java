import program.Program;
import software.PCB;

public class TesteSistemaDemo {
	public static void main(String[] args) {
		System.out.println("=== DEMONSTRAÇÃO COMPLETA DO SO (COMANDOS E PROGRAMAS) ===\n");
		Sistema s = new Sistema(1024, 8, 5);

		// Iniciar threads do sistema para execução contínua
		Thread schedulerThread = new Thread(s.so.scheduler, "Scheduler");
		Thread cpuThread = new Thread(s.hw.cpu, "CPU");
		schedulerThread.setDaemon(true);
		cpuThread.setDaemon(true);
		schedulerThread.start();
		cpuThread.start();

		// 1) Listar programas disponíveis (equivalente a help)
		System.out.println("PROGRAMAS DISPONÍVEIS:");
		System.out.println("nome\t\ttamanho(image)");
		for (Program p : s.so.getPrograms().progs) {
			if (p != null) System.out.printf("%s\t\t%d\n", p.name, p.image.length);
		}
		System.out.println();

		// 2) Criar instâncias de todos os programas (new <nome>)
		String[] nomes = new String[]{
			"fatorial","fatorialV2","fibonacci10","fibonacci10v2","fibonacciREAD","PC","PB","progMinimo"
		};
		int[] pids = new int[nomes.length];
		System.out.println("CRIANDO PROCESSOS (new <nome>):");
		for (int i = 0; i < nomes.length; i++) {
			pids[i] = s.so.newProcess(nomes[i]);
		}
		System.out.println();

		// 3) ps (listar processos)
		System.out.println("PS (processos existentes):");
		System.out.printf("%-5s %-14s %-10s %-5s %-8s\n", "PID","NOME","ESTADO","PC","PAGINAS");
		System.out.println("------------------------------------------------");
		for (PCB pcb : s.so.ps()) {
			System.out.printf("%-5d %-14s %-10s %-5d %-8d\n", pcb.pid, pcb.nome, pcb.state, pcb.pc, pcb.numPages);
		}
		System.out.println();

		// 4) frames (status GM)
		System.out.println(s.so.frames());

		// 5) dump <pid> (dump de um processo)
		int anyPid = -1; for (int pid : pids) { if (pid > 0) { anyPid = pid; break; } }
		if (anyPid > 0) {
			System.out.println("DUMP (dump "+anyPid+"):");
			System.out.println(s.so.dump(anyPid));
		}

		// 6) dumpM <i> <f>
		System.out.println("DUMPM (dumpM 0 64):");
		System.out.println(s.so.dumpM(0, 64));

		// 7) traceOn / traceOff
		System.out.println("TRACEON:");
		s.so.traceOn();
		System.out.println("TRACEOFF:");
		s.so.traceOff();

		// 8) exec <pid> (modo debug não preemptivo) em um processo pequeno (progMinimo)
		int pidProgMinimo = -1; for (int pid : pids) { if (pid > 0) { PCB pcb = s.so.getPCB(pid); if (pcb != null && "progMinimo".equals(pcb.nome)) { pidProgMinimo = pid; break; } } }
		if (pidProgMinimo > 0) {
			System.out.println("EXEC (exec "+pidProgMinimo+"):");
			s.so.exec(pidProgMinimo);
		}

		// 9) rm <id> (remover um processo válido)
		for (int pid : pids) { if (pid > 0 && pid != pidProgMinimo) { System.out.println("RM (rm "+pid+"):"); s.so.rm(pid); break; } }
		System.out.println();

		// 10) execAll (RR até terminar o que restou)
		System.out.println("EXECUÇÃO ESCALONADA (execAll):");
		s.so.execAll();
		System.out.println();

		// 11) ps final
		System.out.println("PS FINAL:");
		System.out.printf("%-5s %-14s %-10s %-5s %-8s\n", "PID","NOME","ESTADO","PC","PAGINAS");
		System.out.println("------------------------------------------------");
		for (PCB pcb : s.so.ps()) {
			System.out.printf("%-5d %-14s %-10s %-5d %-8d\n", pcb.pid, pcb.nome, pcb.state, pcb.pc, pcb.numPages);
		}
		System.out.println();

		// 12) Limpeza final (rm de todos)
		for (int pid : pids) { if (pid > 0) { s.so.rm(pid); } }
		System.out.println("Todos os processos removidos.");
	}
} 