package hardware;

public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
    noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, 
    intTimer, intSysCallStop;
}

