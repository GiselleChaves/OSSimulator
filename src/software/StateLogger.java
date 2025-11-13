package software;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
/**
 * Responsável por registrar mudanças de estado dos processos em um arquivo de log conforme
 * solicitado no enunciado das fases T2a/T2b.
 */
public class StateLogger {
    private final Path logPath;
    
    public StateLogger(Path logPath) {
        try {
            Files.createDirectories(logPath.getParent());
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível inicializar arquivo de log em " + logPath, e);
        }
        this.logPath = logPath;
    }

    private String translateState(PCB.ProcState s) {
        if (s == null) return "nulo";
        switch (s) {
            case READY: return "pronto";
            case RUNNING: return "rodando";
            case BLOCKED: return "bloq";
            case TERMINATED: return "terminado";
            default: return s.toString().toLowerCase(Locale.ROOT);
        }
    }
    
    private String translateReason(String r) {
        if (r == null) return "-";
        switch (r) {
            case "creation":     return "criacao";
            case "dispatch":     return "escalona";
            case "page_fault":   return "pg fault";
            case "page_loaded":  return "fim pg fault";
            case "timer":        return "fatia tempo.";
            case "io_request":   return "io begin";
            case "io_complete":  return "fim io";
            default:             return r;
        }
    }

    public void log(PCB pcb, String reason, PCB.ProcState from, PCB.ProcState to) {
        if (pcb == null) {
            return;
        }

        if (from != null && to != null && from == to) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(pcb.pid).append(" ; ");
        sb.append(pcb.nome != null ? pcb.nome : "desconhecido").append(" ; ");
        sb.append(translateReason(reason)).append(" ; ");
        sb.append(translateState(from)).append(" ; ");
        sb.append(translateState(to)).append(" ; ");
        sb.append(formatPageTable(pcb));
        String line = sb.toString();

        try {
            synchronized (this) {
                try (BufferedWriter writer = Files.newBufferedWriter(
                        logPath,
                        StandardOpenOption.APPEND)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("LOG ERROR: não foi possível escrever no arquivo de log. " + e.getMessage());
        }
    }

    private String formatPageTable(PCB pcb) {
        if (pcb.pageTable == null || pcb.pageTable.length == 0) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < pcb.pageTable.length; i++) {
            PageTableEntry entry = pcb.pageTable[i];
            if (entry == null) {
                continue;
            }
            if (i > 0) {
                sb.append(", ");
            }
            String framePart = entry.valid && entry.frameNumber >= 0 ? Integer.toString(entry.frameNumber) : "-";
            String location;
            if (entry.valid && entry.frameNumber >= 0) {
                location = "mp"; // memória principal
            } else if (entry.diskAddress >= 0) {
                location = "ms"; // memória secundária (disco)
            } else {
                location = "--"; // nunca carregada
            }
            sb.append("[").append(i).append(",").append(framePart).append(",").append(location).append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}


