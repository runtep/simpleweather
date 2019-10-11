package android.util;

public class Log {

    public static int i(String prefix, String msg) {
        System.out.println(prefix + " " + msg);
        return 0;
    }

    public static int w(String prefix, String msg) {
        System.err.println(prefix + " " + msg);
        return 0;
    }
}
