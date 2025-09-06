package util;

// ------- U T I L I T A R I O S D O S I S T E M A
// ------- load é invocado a partir de requisição do usuário

import hardware.Hw;
import hardware.Word;

// carga na memória
public class Utilities {
    private Hw hw;

    public Utilities(Hw _hw) {
        hw = _hw;
    }

    private void loadProgram(Word[] p) {
        Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
        for (int i = 0; i < p.length; i++) {
            m[i].opc = p[i].opc;
            m[i].ra = p[i].ra;
            m[i].rb = p[i].rb;
            m[i].p = p[i].p;
        }
    }

    // dump da memória
    public void dump(Word w) { // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
        System.out.print("[ ");
        System.out.print(w.opc);
        System.out.print(", ");
        System.out.print(w.ra);
        System.out.print(", ");
        System.out.print(w.rb);
        System.out.print(", ");
        System.out.print(w.p);
        System.out.println("  ] ");
    }

    public void dump(int ini, int fim) {
        Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
        for (int i = ini; i < fim; i++) {
            System.out.print(i);
            System.out.print(":  ");
            dump(m[i]);
        }
    }

    public void loadAndExec(Word[] p) {
        loadProgram(p); // carga do programa na memoria
        System.out.println("---------------------------------- programa carregado na memoria");
        dump(0, p.length); // dump da memoria nestas posicoes
        hw.cpu.setContext(0); // seta pc para endereço 0 - ponto de entrada dos programas
        System.out.println("---------------------------------- inicia execucao ");
        hw.cpu.run(); // cpu roda programa ate parar
        System.out.println("---------------------------------- memoria após execucao ");
        dump(0, p.length); // dump da memoria com resultado
    }
}
