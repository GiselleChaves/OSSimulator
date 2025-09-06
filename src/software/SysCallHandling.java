package software;

import hardware.Hw;

// ------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
public class SysCallHandling {
    private Hw hw; // referencia ao hw se tiver que setar algo

    public SysCallHandling(Hw _hw) {
        hw = _hw;
    }

    public void stop() { // chamada de sistema indicando final de programa
        // nesta versao cpu simplesmente pára
        System.out.println("SYSCALL STOP");
    }

    public void handle() { // chamada de sistema
        // suporta somente IO, com parametros
        // reg[8] = in ou out    e reg[9] endereco do inteiro
        System.out.println("SYSCALL para:  " + hw.cpu.getReg()[8] + " / " + hw.cpu.getReg()[9]);

        if  (hw.cpu.getReg()[8]==1){
            // leitura ...

        } else if (hw.cpu.getReg()[8]==2){
            // escrita - escreve o conteuodo da memoria na posicao dada em reg[9]
            System.out.println("OUT:   "+ hw.mem.pos[hw.cpu.getReg()[9]].p);
        } else {System.out.println("  PARAMETRO INVALIDO"); }
    }
}
