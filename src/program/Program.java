package program;

// --------------- P R O G R A M A S - não fazem parte do sistema
// esta classe representa programas armazenados (como se estivessem em disco)
// que podem ser carregados para a memória (load faz isto)

import hardware.Word;

/**
 * Representa um programa “em disco”: um nome e sua imagem (array de Words).
 * O SO carrega páginas desta imagem para a memória conforme necessário (VM).
 */
public class Program {
	public String name;
	public Word[] image;

	public Program(String n, Word[] i) {
		name = n;
		image = i;
	}
}

