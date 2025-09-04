package hardware;

import software.InterruptHandling;
import software.SysCallHandling;
import software.PCB;
import software.SO;
import util.Utilities;

public class CPU implements Runnable {
    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;

    // CONTEXTO da CPU ...
    private int pc;     // ... composto de program counter,
    private Word ir;    // instruction register,

    private int[] reg;  // registradores da CPU
    private Interrupts irpt; // durante instrucao, interrupcao pode ser sinalizada
    // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
    // executa-lo
    // nas proximas versoes isto pode modificar

    private Memory mem;   // referência à memória física

    private InterruptHandling ih;    // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
    private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

    private boolean cpuStop;    // flag para parar CPU - caso de interrupcao que acaba o processo, ou chamada stop -
    // nesta versao acaba o sistema no fim do prog

    // auxilio aa depuração
    private boolean debug;      // se true entao mostra cada instrucao em execucao
    private Utilities u;        // para debug (dump)

    // Controle de preempção por tempo
    private int delta;          // fatia de tempo em número de instruções
    private int instructionCount; // contador de instruções executadas na fatia atual
    
    // Referência ao SO para tradução de endereços
    private SO so;
    
    // Controle de thread
    private boolean active;

    public CPU(Memory _mem, boolean _debug) { // ref a MEMORIA passada na criacao da CPU
        maxInt = 32767;            // capacidade de representacao modelada
        minInt = -32767;           // se exceder deve gerar interrupcao de overflow
        mem = _mem;              // usa o atributo 'mem' para acessar a memoria
        reg = new int[10];         // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO

        debug = _debug;            // se true, print da instrucao em execucao
        
        // Defaults para preempção
        delta = 5;
        instructionCount = 0;
        active = false;
    }

    public void setSO(SO so) {
        this.so = so;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih;                  // aponta para rotinas de tratamento de int
        sysCall = _sysCall;        // aponta para rotinas de tratamento de chamadas de sistema
    }

    public void setUtilities(Utilities _u) {
        u = _u;                     // aponta para rotinas utilitárias - fazer dump da memória na tela
    }

    public void setContext(PCB pcb) {
        pc = pcb.pc;
        System.arraycopy(pcb.reg, 0, reg, 0, reg.length);
        irpt = Interrupts.noInterrupt;
        instructionCount = 0; // Reset contador de instruções
        cpuStop = false;
        debug = pcb.trace;
    }

    public void saveContext(PCB pcb) {
        pcb.pc = pc;
        System.arraycopy(reg, 0, pcb.reg, 0, reg.length);
    }

    private boolean verifyOverflow(int v) {             // toda operacao matematica deve avaliar se ocorre overflow
        if ((v < minInt) || (v > maxInt)) {
            irpt = Interrupts.intOverflow;            // se houver liga interrupcao no meio da exec da instrucao
            return false;
        }
        ;
        return true;
    }

    private boolean isValidAddress(int addr) {
        if (addr < 0 || addr >= mem.getTamMem()) {
            irpt = Interrupts.intEnderecoInvalido;
            return false;
        }
        return true;
    }

    // Tradução de endereço lógico para físico via SO
    private int translateAddress(int logicalAddr) {
        if (so != null) {
            PCB currentPCB = so.scheduler.getRunning();
            if (currentPCB != null) {
                return so.traduzEndereco(currentPCB, logicalAddr);
            }
        }
        return logicalAddr; // Fallback para compatibilidade
    }

    // Acesso à memória com tradução
    private Word readMemory(int logicalAddr) {
        int physicalAddr = translateAddress(logicalAddr);
        if (!isValidAddress(physicalAddr)) {
            irpt = Interrupts.intEnderecoInvalido;
            return new Word(Opcode.___, -1, -1, -1);
        }
        return mem.read(physicalAddr);
    }

    private void writeMemory(int logicalAddr, Word word) {
        int physicalAddr = translateAddress(logicalAddr);
        if (!isValidAddress(physicalAddr)) {
            irpt = Interrupts.intEnderecoInvalido;
            return;
        }
        mem.write(physicalAddr, word);
    }

    public boolean isValidInstruction(Opcode opc) {
        if (opc == null) return false;
        switch(opc) {
            case JMP: case JMPI: case JMPIG: case JMPIL: case JMPIE:
            case JMPIM: case JMPIGM: case JMPILM: case JMPIEM:
            case JMPIGK: case JMPILK: case JMPIEK: case JMPIGT:
            case ADDI: case SUBI: case ADD: case SUB: case MULT:
            case LDI: case LDD: case STD: case LDX: case STX: case MOVE:
            case SYSCALL: case STOP:
                return true;
            default:
                return false;
        }
    }

    public void setContext(int _pc) {                 // usado para setar o contexto da cpu para rodar um processo
        // [ nesta versao é somente colocar o PC na posicao 0 ]
        pc = _pc;                                     // pc cfe endereco logico
        irpt = Interrupts.noInterrupt;                // reset da interrupcao registrada
        instructionCount = 0;
    }

    public void step() {
        if (cpuStop) return;
        
        // --------------------------------------------------------------------------------------------------
        // FASE DE FETCH
        ir = readMemory(pc);  // Usa tradução de endereços
        if (irpt != Interrupts.noInterrupt) {
            return; // Erro de acesso à memória
        }

        // resto é dump de debug
        if (debug) {
            System.out.print("                                              regs: ");
            for (int i = 0; i < 10; i++) {
                System.out.print(" r[" + i + "]:" + reg[i]);
            }
            System.out.println();
        }
        if (debug) {
            System.out.print("                      pc: " + pc + "       exec: ");
            u.dump(ir);
        }

        // --------------------------------------------------------------------------------------------------
        // FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir

        if (!isValidInstruction(ir.opc)) {
            irpt = Interrupts.intInstrucaoInvalida;
            return;
        }

        switch (ir.opc) {       // conforme o opcode (código de operação) executa

            // Instrucoes de Busca e Armazenamento em Memoria
            case LDI: // Rd ← k        veja a tabela de instrucoes do HW simulado para entender a semantica da instrucao
                reg[ir.ra] = ir.p;
                pc++;
                break;
            case LDD: // Rd <- [A]
                Word dataWord = readMemory(ir.p);
                if (irpt == Interrupts.noInterrupt) {
                    reg[ir.ra] = dataWord.p;
                    pc++;
                }
                break;
            case LDX: // RD <- [RS] // NOVA
                Word dataWordX = readMemory(reg[ir.rb]);
                if (irpt == Interrupts.noInterrupt) {
                    reg[ir.ra] = dataWordX.p;
                    pc++;
                }
                break;
            case STD: // [A] ← Rs
                Word storeWord = new Word(Opcode.DATA, -1, -1, reg[ir.ra]);
                writeMemory(ir.p, storeWord);
                if (irpt == Interrupts.noInterrupt) {
                    pc++;
                    if (debug) {
                        System.out.print("                                  ");
                        u.dump(ir.p, ir.p + 1);
                    }
                }
                break;
            case STX: // [Rd] ←Rs
                Word storeWordX = new Word(Opcode.DATA, -1, -1, reg[ir.rb]);
                writeMemory(reg[ir.ra], storeWordX);
                if (irpt == Interrupts.noInterrupt) {
                    pc++;
                }
                break;
            case MOVE: // RD <- RS
                reg[ir.ra] = reg[ir.rb];
                pc++;
                break;
            // Instrucoes Aritmeticas
            case ADD: // Rd ← Rd + Rs
                reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                if (!verifyOverflow(reg[ir.ra])) {
                    return;
                }
                pc++;
                break;
            case ADDI: // Rd ← Rd + k
                reg[ir.ra] = reg[ir.ra] + ir.p;
                if (!verifyOverflow(reg[ir.ra])) {
                    return;
                }
                pc++;
                break;
            case SUB: // Rd ← Rd - Rs
                reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                if (!verifyOverflow(reg[ir.ra])) {
                    return;
                }
                pc++;
                break;
            case SUBI: // RD <- RD - k // NOVA
                reg[ir.ra] = reg[ir.ra] - ir.p;
                if (!verifyOverflow(reg[ir.ra])) {
                    return;
                }
                pc++;
                break;
            case MULT: // Rd <- Rd * Rs
                reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                if (!verifyOverflow(reg[ir.ra])) {
                    return;
                }
                pc++;
                break;

            // Instrucoes JUMP
            case JMP: // PC <- k
                if (!isValidAddress(ir.p)) {
                    return;
                }
                pc = ir.p;
                break;
            case JMPIM: // PC <- [A]
                Word jumpAddr = readMemory(ir.p);
                if (irpt == Interrupts.noInterrupt) {
                    int destIM = jumpAddr.p;
                    if (!isValidAddress(destIM)) {
                        return;
                    }
                    pc = destIM;
                }
                break;
            case JMPIG: // If Rc > 0 Then PC <- Rs Else PC <- PC +1
                if (reg[ir.rb] > 0) {
                    if (!isValidAddress(reg[ir.ra])) {
                        return;
                    }
                    pc = reg[ir.ra];
                } else {
                    pc++;
                }
                break;

            case JMPIGK: // If Rc > 0 then PC <- k else PC++
                if (reg[ir.rb] > 0) {
                    if (!isValidAddress(ir.p)) {
                        return;
                    }
                    pc = ir.p;
                } else {
                    pc++;
                }
                break;
            case JMPILK: // If Rc < 0 then PC <- k else PC++
                if (reg[ir.rb] < 0) {
                    if (!isValidAddress(ir.p)) {
                        return;
                    }
                    pc = ir.p;
                } else {
                    pc++;
                }
                break;
            case JMPIEK: // If Rc = 0 then PC <- k else PC++
                if (reg[ir.rb] == 0) {
                    if (!isValidAddress(ir.p)) {
                        return;
                    }
                    pc = ir.p;
                } else {
                    pc++;
                }
                break;
            case JMPIL: // If Rc < 0 Then PC <- Rs Else PC <- PC +1
                if (reg[ir.rb] < 0) {
                    if (!isValidAddress(reg[ir.ra])) {
                        return;
                    }
                    pc = reg[ir.ra];
                } else {
                    pc++;
                }
                break;
            case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                if (reg[ir.rb] == 0) {
                    if (!isValidAddress(reg[ir.ra])) {
                        return;
                    }
                    pc = reg[ir.ra];
                } else {
                    pc++;
                }
                break;
            case JMPIGM: // If Rc > 0 then PC <- [A] else PC++
                if (reg[ir.rb] > 0) {
                    Word jumpAddrIGM = readMemory(ir.p);
                    if (irpt == Interrupts.noInterrupt) {
                        int destIGM = jumpAddrIGM.p;
                        if (!isValidAddress(destIGM)) {
                            return;
                        }
                        pc = destIGM;
                    }
                } else {
                    pc++;
                }
                break;

            case JMPILM: // If Rc < 0 then PC <- [A] else PC++
                if (reg[ir.rb] < 0) {
                    Word jumpAddrILM = readMemory(ir.p);
                    if (irpt == Interrupts.noInterrupt) {
                        int destILM = jumpAddrILM.p;
                        if (!isValidAddress(destILM)) {
                            return;
                        }
                        pc = destILM;
                    }
                } else {
                    pc++;
                }
                break;
            case JMPIEM: // If Rc = 0 then PC <- [A] else PC++
                if (reg[ir.rb] == 0) {
                    Word jumpAddrIEM = readMemory(ir.p);
                    if (irpt == Interrupts.noInterrupt) {
                        int destIEM = jumpAddrIEM.p;
                        if (!isValidAddress(destIEM)) {
                            return;
                        }
                        pc = destIEM;
                    }
                } else {
                    pc++;
                }
                break;
            case JMPIGT: // If Rs > Rc then PC <- k else PC++
                if (reg[ir.ra] > reg[ir.rb]) {
                    if (!isValidAddress(ir.p)) {
                        return;
                    }
                    pc = ir.p;
                } else {
                    pc++;
                }
                break;
            case DATA:
                irpt = Interrupts.intInstrucaoInvalida;
                break;

            // Chamadas de sistema
            case SYSCALL:
                sysCall.handle(); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so
                // temos IO
                pc++;
                break;

            case STOP: // por enquanto, para execucao
                irpt = Interrupts.intSysCallStop;
                break;

            // Inexistente
            default:
                irpt = Interrupts.intInstrucaoInvalida;
                break;
        }

        // Incrementar contador de instruções e verificar preempção
        if (irpt == Interrupts.noInterrupt) {
            instructionCount++;
            if (instructionCount >= delta) {
                irpt = Interrupts.intTimer;
                instructionCount = 0;
            }
        }

        // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
        if (irpt != Interrupts.noInterrupt) { // existe interrupção
            ih.handle(irpt);                  // desvia para rotina de tratamento - esta rotina é do SO
            irpt = Interrupts.noInterrupt;    // Reset interrupção após tratamento
        }
    }

    public void run() {                               // execucao da CPU supoe que o contexto da CPU, vide acima,
        // esta devidamente setado
        active = true;
        while (active) {
            PCB currentPCB = (so != null && so.scheduler != null) ? so.scheduler.getRunning() : null;
            if (currentPCB != null && currentPCB.state == PCB.ProcState.RUNNING) {
                step();
            } else {
                // CPU aguarda processo para executar
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void stopCPU() {
        active = false;
        cpuStop = true;
    }

    public int getMaxInt() {
        return maxInt;
    }

    public int getPc() {
        return pc;
    }

    public int[] getReg() {
        return reg;
    }

    public int getDelta() {
        return delta;
    }

    public int getInstructionCount() {
        return instructionCount;
    }
}
