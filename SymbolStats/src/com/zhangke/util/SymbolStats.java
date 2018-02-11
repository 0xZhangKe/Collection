package com.zhangke.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    private String targetDir = "/home/zhangke/";
    private char[] hitCharArray = new char[26 * 2];
    /**
     * 匹配是否为文本文件
     */
    final Pattern mFileNamePattern = Pattern.compile("^.*?\\.(java|py|c|cpp|xml|html|cs|bat|shell|sql|js|)$");

    /**
     * 文件个数
     */
    private int mFileCount = 0;

    private CountDownLatch mCountDownLatch = new CountDownLatch(DEFAULT_POLL_SIZE);
    private LinkedBlockingQueue<File> mFileQueue = new LinkedBlockingQueue<>();

    private ComputeSymbolThread[] computeSymbolThreadPool = new ComputeSymbolThread[DEFAULT_POLL_SIZE];
    private Runnable loopDirRunnable = new Runnable() {
        @Override
        public void run() {
            loopDir(targetDir);
            loopEnd = true;
            System.out.println(String.format("File count：%s", mFileCount));
        }
    };

    /**
     * 文件夹是否遍历完成
     */
    private boolean loopEnd;

    public SymbolStats() {
        long start = System.currentTimeMillis();
        int count = 0;
        for (int i = 'a'; i <= 'z'; i++) {
            hitCharArray[count++] = (char) i;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            hitCharArray[count++] = (char) i;
        }

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
            System.out.println(String.format("Total line：%s", codeLineCount));
            System.out.println(String.format("Taking %s ms", System.currentTimeMillis() - start));
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
            mFileQueue.put(file);
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

        /**
         * 获取当前线程检测到的代码行数
         */
        int getCodeLineCount() {
            return codeLineCount;
        }
    }

    public static void main(String[] args) {
        SymbolStats symbolStats = new SymbolStats();
    }
}
