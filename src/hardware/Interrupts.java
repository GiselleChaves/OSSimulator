package hardware;

public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
    noInterrupt,
    intPageFault,
    intEnderecoInvalido,
    intInstrucaoInvalida,
    intOverflow,
    intTimer,
    intSysCallStop;
}

