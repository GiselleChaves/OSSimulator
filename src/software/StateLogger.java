package software;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Responsável por registrar mudanças de estado dos processos em um arquivo de log conforme
 * solicitado no enunciado das fases T2a/T2b.
 */
public class StateLogger {
    private final Path logPath;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

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

    public void log(PCB pcb, String reason, PCB.ProcState from, PCB.ProcState to) {
        if (pcb == null) {
            return;
        }

        String timestamp = timestampFormat.format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(" ; ");
        sb.append(pcb.pid).append(" ; ");
        sb.append(pcb.nome != null ? pcb.nome : "desconhecido").append(" ; ");
        sb.append(reason != null ? reason : "-").append(" ; ");
        sb.append(from != null ? from : "NULL").append(" ; ");
        sb.append(to != null ? to : "NULL").append(" ; ");
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


