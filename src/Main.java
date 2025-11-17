/**
 * Ponto de entrada do simulador.
 * - Aceita parâmetros de linha de comando para memória, tamanho de página e delta.
 * - Instancia e executa o `Sistema` (que iniciará todas as threads).
 *
 * Dica em apresentação: rode com --help para ver exemplos e flags disponíveis.
 */
public class Main {
    public static void main(String[] args) {
        // Parâmetros default
        int tamMem = 1024;
        int tamPg = 8;
        int delta = 5;
        
        // Parse dos argumentos de linha de comando
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mem":
                    if (i + 1 < args.length) {
                        tamMem = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--page":
                    if (i + 1 < args.length) {
                        tamPg = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--delta":
                    if (i + 1 < args.length) {
                        delta = Integer.parseInt(args[++i]);
                    }
                    break;

                case "--help":
                case "-h":
                    printHelp();
                    return;
                default:
                    System.out.println("Parâmetro desconhecido: " + args[i]);
                    printHelp();
                    return;
            }
        }
        
        try {
            Sistema s = new Sistema(tamMem, tamPg, delta);
            s.run();
        } catch (Exception e) {
            System.err.println("Erro ao inicializar sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printHelp() {
        // Ajuda simples com parâmetros e exemplos de execução
        System.out.println("SO Didático - Simulador de Sistema Operacional");
        System.out.println();
        System.out.println("Uso: java Main [opções]");
        System.out.println();
        System.out.println("Opções:");
        System.out.println("  --mem <tamanho>    Tamanho da memória em palavras (default: 1024)");
        System.out.println("  --page <tamanho>   Tamanho da página em palavras (default: 8)");
        System.out.println("  --delta <valor>    Fatia de tempo em nº de instruções (default: 5)");

        System.out.println("  --help, -h         Mostrar esta ajuda");
        System.out.println();
        System.out.println("Exemplos:");
        System.out.println("  java Main                                     # Shell interativo");
        System.out.println("  java Main --mem 2048 --page 16 --delta 8     # Parâmetros customizados");
    }
}