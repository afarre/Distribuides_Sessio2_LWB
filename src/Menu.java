import java.util.InputMismatchException;
import java.util.Scanner;

public class Menu {

    public int showMenu() {
        int opcio;
        do {
            System.out.println("Sel·lecciona la opcio corresponent:");
            System.out.println("\t1. Versio 0 - Sockets");
            System.out.println("\t2. Versio 1 - Shared Memory");
            opcio = readInt();
        } while (opcio < 0 || opcio > 2);

        return opcio;
    }


    /**
     * Comprova que l'usuari introduiex un enter
     * @return El numero introduit per l'usuari o -1 en cas de que no hagi introduit un numero
     */
    private int readInt(){
        try {
            Scanner read = new Scanner(System.in);
            return read.nextInt();
        }catch (InputMismatchException ignored){
        }
        return -1;
    }
}
