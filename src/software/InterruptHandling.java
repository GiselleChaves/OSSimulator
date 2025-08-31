package software;

import hardware.Hw;
import hardware.Interrupts;

// ------- I N T E R R U P C O E S - rotinas de tratamento ------
public class InterruptHandling {
    private Hw hw; // referencia ao hw se tiver que setar algo

    public InterruptHandling(Hw _hw) {
        hw = _hw;
    }

    public void handle(Interrupts irpt) {
        // apenas avisa - todas interrupcoes neste momento finalizam o programa
        System.out.println(
                "Interrupcao " + irpt + "   pc: " + hw.cpu.getPc());
    }
}
