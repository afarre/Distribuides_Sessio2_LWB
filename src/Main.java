public class Main {
    public static void main(String[] args) {
        S_LWB s_lwb = new S_LWB(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        s_lwb.start();
    }
}
