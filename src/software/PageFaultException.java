package software;

/**
 * Exceção lançada durante a tradução de endereços quando é detectado um page fault.
 * O disparo desta exceção interrompe a instrução corrente da CPU, permitindo que
 * o processo seja bloqueado e que a operação de paginação ocorra de forma assíncrona.
 */
public class PageFaultException extends RuntimeException {
    private final int pid;
    private final int pageNumber;

    public PageFaultException(int pid, int pageNumber, String message) {
        super(message);
        this.pid = pid;
        this.pageNumber = pageNumber;
    }

    public PageFaultException(int pid, int pageNumber) {
        this(pid, pageNumber, null);
    }

    public int getPid() {
        return pid;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}


