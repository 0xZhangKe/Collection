package com.zhangke.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolStats {

    /**
     * 默认线程个数
     */
    private static final int DEFAULT_POLL_SIZE = 4;

    /**
     * 目标文件夹
     */
    private String targetDir = "D:\\Mine";
    private final HashMap<String, Integer> hitSymbolMap = new HashMap<>();
    /**
     * 匹配是否为文本文件
     */
    final Pattern mFileNamePattern = Pattern.compile("^.*?\\.(java|py|c|cpp|html|cs|bat|shell|sql|js|)$");

    /**
     * 文件个数
     */
    private int mFileCount = 0;

    private CountDownLatch mCountDownLatch = new CountDownLatch(DEFAULT_POLL_SIZE);
    private final LinkedBlockingQueue<File> mFileQueue = new LinkedBlockingQueue<>();

    private ComputeSymbolThread[] computeSymbolThreadPool = new ComputeSymbolThread[DEFAULT_POLL_SIZE];
    private Runnable loopDirRunnable = new Runnable() {
        @Override
        public void run() {
            loopDir(targetDir);
            loopEnd = true;
            loopDirEndTime = System.currentTimeMillis();
        }
    };

    /**
     * 文件夹是否遍历完成
     */
    private boolean loopEnd;

    private long loopDirEndTime;

    public SymbolStats() {
        long startTime = System.currentTimeMillis();

        loopEnd = false;
        new Thread(loopDirRunnable).start();
        for (int i = 0; i < computeSymbolThreadPool.length; i++) {
            ComputeSymbolThread computeSymbolThread = new ComputeSymbolThread(mFileQueue);
            computeSymbolThreadPool[i] = computeSymbolThread;
            computeSymbolThread.start();
        }
        try {
            mCountDownLatch.await();
            int codeLineCount = 0;
            for (int i = 0; i < computeSymbolThreadPool.length; i++) {
                codeLineCount += computeSymbolThreadPool[i].getCodeLineCount();
            }

            System.out.println(String.format("\t%s text files.", mFileCount));
            System.out.println(String.format("\t%s lines code.", codeLineCount));
            long taking = System.currentTimeMillis() - startTime;
            double fileTaking = ((double) mFileCount) / ((double) loopDirEndTime) * 1000.0;
            double lineTaking = ((double) codeLineCount) / ((double) taking) * 1000.0;

            System.out.println(String.format("\tT=%s ms ( %.1f files/s, %.1f lines/s)",
                    taking,
                    fileTaking,
                    lineTaking));

            List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(hitSymbolMap.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o2.getValue() - o1.getValue();
                }
            });
            int count = 1;
            for (Map.Entry<String, Integer> mapping : list) {
                System.out.println(String.format("\t%s\t%s\t%s", count, mapping.getKey(), mapping.getValue()));
                if (count > 9) break;
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据文件夹遍历下面的子文件
     *
     * @param dirPath 文件夹路径
     */
    private void loopDir(String dirPath) {
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

    /**
     * 筛选出合适的文件
     * TODO 后面可以改为通过 main 函数的参数接收筛选条件
     */
    private boolean filterFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        if (mFileNamePattern.matcher(name).find()) {
            return true;
        }
        return false;
    }

    /**
     * 向阻塞队列中添加一个 File；
     * 该方法可能会阻塞。
     */
    private void addFileToQueue(final File file) {
        try {
            synchronized (mFileQueue) {
                mFileQueue.put(file);
            }
            mFileCount++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ComputeSymbolThread extends Thread {

        private LinkedBlockingQueue<File> mFileQueue;
        /**
         * 代码行数
         */
        private int codeLineCount = 0;

        ComputeSymbolThread(LinkedBlockingQueue<File> fileQueue) {
            this.mFileQueue = fileQueue;
        }

        @Override
        public void run() {
            while (true) {
                File file = mFileQueue.poll();
                if (file == null) {
                    if (loopEnd) {
                        mCountDownLatch.countDown();
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
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        codeLineCount++;
                        line = line.trim();
                        if (line != null && line.length() > 0) {
                            if (line.contains(" ")) {
                                String[] symbolArray = line.split(" ");
                                for (String symbol : symbolArray) {
                                    addWord(symbol);
                                }
                            } else {
                                addWord(line);
                            }
                        }
                    }
                    reader.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
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
            }
        }

        private void addWord(String str) {
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
         * 获取当前线程检测到的代码行数
         */
        int getCodeLineCount() {
            return codeLineCount;
        }

        /**
         * 判断当前字符串是否由字母组成
         */
        private boolean isLetter(String str) {
            if (str == null || str.length() <= 0) {
                return false;
            }
            char[] chars = str.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (!(chars[i] >= 'a' && chars[i] <= 'z') || (chars[i] >= 'A' && chars[i] <= 'Z')) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        SymbolStats symbolStats = new SymbolStats();
    }
}
