import socket.S_LWB;
import xarxa.X_LWB;

public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        int opcio = menu.showMenu();

        if (opcio == 1){
            S_LWB s_lwb = new S_LWB(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            s_lwb.start();
        }else {
            X_LWB x_lwb = new X_LWB(args[0], Integer.parseInt(args[2]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            x_lwb.start();
        }
    }
}
