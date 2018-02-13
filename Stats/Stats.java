
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * ͳ�ƴ�������������Ƶ��
 * Created by ZhangKe on 2018/2/12.
 */
public class Stats {

    /**
     * Ĭ���̸߳���
     */
    private static final int DEFAULT_POLL_SIZE = 4;
    /**
     * ʹ�� HashMap ���浥�ʼ����ִ���
     */
    private final HashMap<String, Integer> hitSymbolMap = new HashMap<>();
    /**
     * ʹ������ƥ���Ƿ�ΪԴ���ļ�
     */
    private Pattern mFileNamePattern = Pattern.compile("^.*?\\.(java|py|c|cpp|cc|cs|sql|js|php|)$");
    /**
     * ��ͳ�Ƶ��ļ�����
     */
    private int mFileCount = 0;
    /**
     * ʹ���������б����Ѽ�⵽���ļ�
     */
    private final LinkedBlockingQueue<File> mFileQueue = new LinkedBlockingQueue<>();
    /**
     * �ļ����Ƿ�������
     */
    private boolean loopEnd;

    public Stats(String[] args) {
        String help = "�÷���java Stats [�ļ���]\n" +
                "   java Stats [�ļ���]\n" +
                "   java Stats -f [.��׺��] ����⵱ǰĿ¼����Ŀ¼������ָ����׺�����ļ���\n" +
                "   java Stats [.] ����⵱ǰĿ¼�����к�׺��Ϊ��java|py|c|cpp|cc|cs|sql|js|php ���ļ���";
        if (args == null || args.length == 0) {
            System.out.println(help);
        } else if (args.length == 1) {
            String arg = args[0];
            if (".".equals(arg)) {
                targetIsDirectory(System.getProperty("user.dir"));//��ǰ����·��
            } else {
                File file = new File(arg);
                if (!file.exists()) {
                    System.out.println("�ļ�������");
                    return;
                }
                if (file.isDirectory()) {
                    targetIsDirectory(arg);
                } else {
                    targetIsFile(arg);
                }
            }
        } else if (args.length == 2 && "-f".equals(args[0]) && args[1].startsWith(".") && args[1].length() > 1) {
            mFileNamePattern = Pattern.compile(String.format("^.*?\\.%s$", args[1].replaceAll("\\.", "")));
            targetIsDirectory(System.getProperty("user.dir"));
        } else {
            System.out.println(String.format("Unrecognized option\n\n%s", help));
        }
    }

    /**
     * ָ���Ĳ���Ϊ�ļ�
     *
     * @param filePath ָ�����ļ�
     */
    private void targetIsFile(String filePath) {
        long startTime = System.currentTimeMillis();
        loopEnd = false;
        addFileToQueue(new File(filePath));
        loopEnd = true;
        ComputeSymbolThread computeSymbolThread = new ComputeSymbolThread(mFileQueue, new CountDownLatch(1));
        computeSymbolThread.start();
        try {
            computeSymbolThread.join();
            printResult(startTime, computeSymbolThread);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * ָ���Ĳ���Ϊ�ļ���
     *
     * @param dirPath ָ�����ļ���
     */
    private void targetIsDirectory(String dirPath) {
        long startTime = System.currentTimeMillis();

        loopEnd = false;
        new LoopDirectoryThread(dirPath).start();

        CountDownLatch countDownLatch = new CountDownLatch(DEFAULT_POLL_SIZE);
        ComputeSymbolThread[] computeSymbolThreadPool = new ComputeSymbolThread[DEFAULT_POLL_SIZE];
        for (int i = 0; i < computeSymbolThreadPool.length; i++) {
            ComputeSymbolThread computeSymbolThread = new ComputeSymbolThread(mFileQueue, countDownLatch);
            computeSymbolThreadPool[i] = computeSymbolThread;
            computeSymbolThread.start();
        }
        try {
            countDownLatch.await();
            printResult(startTime, computeSymbolThreadPool);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ��ӡͳ�ƽ��
     */
    private void printResult(long startTime, ComputeSymbolThread... threads) {
        int codeLineCount = 0;

        for (ComputeSymbolThread thread : threads) {
            codeLineCount += thread.getCodeLineCount();
        }

        System.out.println(String.format("\t%s text files.", mFileCount));
        System.out.println(String.format("\t%s lines code.", codeLineCount));
        long taking = System.currentTimeMillis() - startTime;
        double fileTaking = ((double) mFileCount) / ((double) taking) * 1000.0;
        double lineTaking = ((double) codeLineCount) / ((double) taking) * 1000.0;

        System.out.println(String.format("\tT=%s ms ( %.1f files/s, %.1f lines/s)",
                taking,
                fileTaking,
                lineTaking));
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(hitSymbolMap.entrySet());
        list.sort(new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        int count = 1;
        System.out.println("\n\ttop\tword\ttimes\t");
        for (Map.Entry<String, Integer> mapping : list) {
            System.out.println(String.format("\t%s\t%s\t%s", count, mapping.getKey(), mapping.getValue()));
            if (count > 9) break;
            count++;
        }
    }

    /**
     * ɸѡ�����ʵ��ļ�
     */
    private boolean filterFile(File file) {
        return file != null && !file.isDirectory() && mFileNamePattern.matcher(file.getName().toLowerCase()).find();
    }

    /**
     * ���������������һ�� File
     */
    private void addFileToQueue(final File file) {
        try {
            synchronized (mFileQueue) {
                mFileQueue.put(file);
            }
            mFileCount++;
        } catch (InterruptedException e) {
            //��������
            e.printStackTrace();
        }
    }

    /**
     * �����ļ����߳�
     */
    private class LoopDirectoryThread extends Thread {
        private String dirPath;

        LoopDirectoryThread(String dirPath) {
            this.dirPath = dirPath;
        }

        @Override
        public void run() {
            try {
                loopDir(dirPath);
            } finally {
                loopEnd = true;
            }
        }

        /**
         * �����ļ��б�����������ļ�
         *
         * @param dirPath �ļ���·��
         */
        private void loopDir(String dirPath) {
            if (dirPath == null || dirPath.isEmpty()) {
                System.out.println("directory is empty");
                return;
            }
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                System.out.println(String.format("%s file not exists", dirPath));
                return;
            }
            if (dirFile.isDirectory()) {
                File[] files = dirFile.listFiles();
                if (files != null && files.length > 0) {
                    for (File item : files) {
                        if (item.isDirectory()) {
                            loopDir(item.getPath());
                        } else if (filterFile(item)) {
                            addFileToQueue(item);
                        }
                    }
                }
            } else {
                System.out.println(String.format("%s must a folder!", dirPath));
            }
        }
    }

    /**
     * �����ַ����߳�
     */
    private class ComputeSymbolThread extends Thread {

        private CountDownLatch mCountDown;
        private LinkedBlockingQueue<File> mFileQueue;
        /**
         * ��ǰ�߳�ͳ�ƵĴ�������
         */
        private int codeLineCount = 0;

        ComputeSymbolThread(LinkedBlockingQueue<File> fileQueue, CountDownLatch countDown) {
            this.mFileQueue = fileQueue;
            this.mCountDown = countDown;
        }

        @Override
        public void run() {
            while (true) {
                File file = mFileQueue.poll();
                try {
                    if (file == null) {
                        if (loopEnd) {
                            mCountDown.countDown();
                            return;
                        } else {
                            continue;
                        }
                    }
                    BufferedReader reader = null;
                    FileReader fileReader = null;
                    try {
                        fileReader = new FileReader(file);
                        reader = new BufferedReader(fileReader);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            codeLineCount++;
                            line = line.trim();
                            if (line.length() > 0) {
                                if (line.contains(" ")) {
                                    String[] symbolArray = line.split(" ");
                                    for (String symbol : symbolArray) {
                                        addWordToMap(symbol);
                                    }
                                } else {
                                    addWordToMap(line);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println(e.toString());
                        mFileCount--;
                        addFileToQueue(file);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e1) {
                                System.out.println(e1.toString());
                            }
                        }
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (IOException e1) {
                                System.out.println(e1.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.toString());
                    mFileCount--;
                    addFileToQueue(file);
                }
            }
        }

        private void addWordToMap(String str) {
            if (isLetter(str)) {
                synchronized (hitSymbolMap) {
                    int count;
                    if (hitSymbolMap.containsKey(str)) {
                        count = hitSymbolMap.get(str) + 1;
                    } else {
                        count = 1;
                    }
                    hitSymbolMap.put(str, count);
                }
            }
        }

        /**
         * ��ȡ��ǰ�̼߳�⵽�Ĵ�������
         */
        int getCodeLineCount() {
            return codeLineCount;
        }

        /**
         * �жϵ�ǰ�ַ����Ƿ�����ĸ���
         */
        private boolean isLetter(String str) {
            if (str == null || str.length() <= 0 || str.trim().length() <= 0) {
                return false;
            }
            char[] chars = str.toCharArray();
            for (char c : chars) {
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        Stats stats = new Stats(args);
    }
}
