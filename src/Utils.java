import java.io.File;

public class Utils {

    private static final String[] sizeUnits = new String[] {"B", "KB", "MB", "GB"};

    /**
     * Convert bytes into higher grades
     * @param sizeBytes size to convert
     * @return String with converted value and unit
     */
    static String bytesSize(long sizeBytes) {
        double size = sizeBytes;
        int unitIndex = 0;
        while (size > 1024 && unitIndex < (sizeUnits.length - 1)) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, sizeUnits[unitIndex]);
    }

    /**
     * Convert byte array into HEX string
     * @param bytes to convert
     * @return hex formatted String of input bytes
     */
    static String byte2hex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    /**
     * Debug log into the System.out.println()
     * @param msg to log
     */
    static void logDebug(String msg) {
        if (!Config.DEBUG_LOG) return;
        System.out.println(msg);
    }

    private static void hwInfo() {

        Utils.logDebug("Available processors (cores): "+ Utils.bytesSize(Runtime.getRuntime().availableProcessors()));

        Utils.logDebug("Free memory (bytes): "+ Utils.bytesSize(Runtime.getRuntime().freeMemory()));

        long maxMemory = Runtime.getRuntime().maxMemory();
        Utils.logDebug("Maximum memory (bytes): "+(maxMemory == Long.MAX_VALUE ? "no limit" : Utils.bytesSize(maxMemory)));

        Utils.logDebug("Total memory (bytes): "+ Utils.bytesSize(Runtime.getRuntime().totalMemory()));

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            Utils.logDebug("    File system root: "+root.getAbsolutePath());
            Utils.logDebug(" Total space (bytes): "+ Utils.bytesSize(root.getTotalSpace()));
            Utils.logDebug("  Free space (bytes): "+ Utils.bytesSize(root.getFreeSpace()));
            Utils.logDebug("Usable space (bytes): "+ Utils.bytesSize(root.getUsableSpace()));
        }

        Utils.logDebug("");
    }

}
