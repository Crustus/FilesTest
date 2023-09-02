import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test {

    /**
     * Chunk size in bytes used in InputStream buffer
     * TODO optimize size to utilize cache
     */
    private static final int CHUNK_SIZE_BYTES = 1024 * 64;

    /**
     * Sync lock to update Groups
     */
    private static final Object groupsLock = new Object();
    /**
     * File groups
     */
    private static final HashMap<String, List<File>> groups = new HashMap<>();



    public static void main(String[] args) {

        //hwInfo();

        long startMs = System.currentTimeMillis();

        if (args.length != 2) {
            System.err.println("Usage: TestProgram <N> <path to the file directory>");
            System.exit(ErrorCode.ARGS);
        }

        int numThreads = 1;
        try {
            numThreads = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException ignored) {}

        if (numThreads < 1 || numThreads > 255) {
            System.err.println("Invalid thread count.");
            System.exit(ErrorCode.INVALID_THREAD_COUNT);
        }
        //numThreads = 12;

        String directoryPath = args[1];
        // 127 files, 35 GB .. approx 24 sec
        //directoryPath = "E:\\Hry\\Fortnite\\FortniteGame\\Content\\Paks";

        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            System.err.println("Invalid directory path.");
            System.exit(ErrorCode.INVALID_DIRECTORY_PATH);
        }

        File[] files = directory.listFiles();

        if (files == null) {
            System.err.println("Failed to list files in the directory.");
            System.exit(ErrorCode.FAILED_LIST_FILES);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (File file : files) {
            if (file.isFile()) {
                //if (DEBUG_LOG) logDebug("file: "+file+", size: "+bytesSize(file.length()));
                executor.submit(() -> {
                    checkFile(file);
                });
            }
        }

        //logDebug("all files sent to the Executor");

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println("Thread pool was interrupted.");
            System.exit(ErrorCode.THREAD_POOL_INTERRUPTED);
        }

        // print output
        for (Map.Entry<String, List<File>> group: groups.entrySet()) {
            Utils.logDebug("Group: "+group.getKey());
            List<File> values = group.getValue();
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) System.out.print(", ");
                System.out.print(values.get(index).getName());
            }
            System.out.println();
        }

        System.out.println("---END---");

        // some execution time measurement
        long durationMs = System.currentTimeMillis() - startMs;
        System.out.println("job done, "+durationMs+" ms");

    }

    private static void checkFile(File file) {

        long startMs = System.currentTimeMillis();

        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            //log("checkFile start\n - "+file.getName()+", "+bytesSize(file.length()));

            byte[] buffer = new byte[CHUNK_SIZE_BYTES];
            int bytesRead;

            // TODO collisions, cryptographic precision not needed
            MessageDigest checkAlg = MessageDigest.getInstance("SHA-1");
            // ZIPs Adler would be enough
            //Adler32 checkAlg = new Adler32();

            while ((bytesRead = bufferedInputStream.read(buffer, 0, CHUNK_SIZE_BYTES)) != -1) {
                checkAlg.update(buffer, 0, bytesRead);
            }

            byte[] digest = checkAlg.digest();
            //byte[] digest = BigInteger.valueOf(checkAlg.getValue()).toByteArray();    // for Adler
            String checksum = Utils.byte2hex(digest);

            long durationMs = System.currentTimeMillis() - startMs;

            synchronized (groupsLock) {
                Utils.logDebug("synced "+Thread.currentThread().getName());
                List<File> group = groups.get(checksum);
                if (group == null) {
                    groups.put(checksum, new ArrayList<>() {{ add(file); }});
                }
                else {
                    group.add(file);
                }
            }

            if (Config.DEBUG_LOG) {
                Utils.logDebug(file.getName() + ", " + Utils.bytesSize(file.length()) + ", " + checksum + ", T " + Thread.currentThread().getName() + ", duration " + durationMs + " ms");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
