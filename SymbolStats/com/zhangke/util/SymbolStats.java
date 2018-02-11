package com.zhangke.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class SymbolStats {

    private char[] targetCharArray = new char[26 * 2];
    private LinkedBlockingQueue<File> mFileQueue = new LinkedBlockingQueue<>();
    private ComputeSymbolThread[] computeSymbolThreadPool = new ComputeSymbolThread[4];
    private Runnable loopDirRunnable = new Runnable() {
        @Override
        public void run() {
            loopDir("D:\\Mine");
        }
    };

    public SymbolStats() {
        int count = 0;
        for (int i = 'a'; i <= 'z'; i++) {
            targetCharArray[count++] = (char) i;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            targetCharArray[count++] = (char) i;
        }

        new Thread(loopDirRunnable).start();

        for (int i = 0; i < computeSymbolThreadPool.length; i++) {
            ComputeSymbolThread computeSymbolThread = new ComputeSymbolThread(mFileQueue);
            computeSymbolThreadPool[i] = computeSymbolThread;
            computeSymbolThread.start();
        }
    }

    /**
     * 根据文件夹遍历下面的子文件
     *
     * @param dirPath 文件夹路径
     */
    private void loopDir(String dirPath) {
        File dirFile = new File(dirPath);
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
        }
    }

    /**
     * 筛选出合适的文件
     * TODO 后面可以改为通过 main 函数接收筛选条件
     */
    private boolean filterFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        if (file.getName().endsWith("java")) {
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ComputeSymbolThread extends Thread {

        private LinkedBlockingQueue<File> mFileQueue;

        ComputeSymbolThread(LinkedBlockingQueue<File> fileQueue) {
            this.mFileQueue = fileQueue;
        }

        @Override
        public void run() {
            try {
                File file = mFileQueue.take();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line = null;
                    while ((line = reader.readLine()) != null) {

                    }
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SymbolStats symbolStats = new SymbolStats();

    }
}
