package hardware;

public class Word {    // cada posicao da memoria tem uma instrucao (ou um dado)
    public Opcode opc; //
    public int ra;     // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
    public int rb;     // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
    public int p;      // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

    public Word(Opcode opc, int ra, int rb, int p) { // vide definição da VM - colunas vermelhas da tabela
        opc = opc;
        ra = ra;
        rb = rb;
        p  = p;
    }
}
