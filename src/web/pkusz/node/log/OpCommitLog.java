package web.pkusz.node.log;

import web.pkusz.node.ConfigNode;

import java.io.*;

/**
 * Created by nick on 2018/1/6.
 */
public class OpCommitLog {

    File file;

    public static class Entry {
        long commitOpNum;
        int state;

        public Entry(long commitOpNum, int state) {
            this.commitOpNum = commitOpNum;
            this.state = state;
        }

        public long getCommitOpNum() {
            return commitOpNum;
        }

        public int state() {
            return state;
        }
    }

    public OpCommitLog() {
        File logFile = new File(ConfigNode.COMMIT_LOG);
        if (!logFile.exists()) {
            try {
                if(!logFile.createNewFile())
                    //log
                    throw new RuntimeException("commit log create failed.");
            } catch (IOException e) {
                //log
                System.out.println(e);
                throw new RuntimeException("commit log create failed.");
            }
        }
        file = logFile;
        write(0, 0);
    }

    public boolean write(long commitOpNum, int state) {
        String content = commitOpNum + ":" + state;
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
            pw.println(content);
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException ioe) {
                System.out.println(ioe);
                System.out.println("commit log create failed.");
            }
            return false;
        }
        return true;
    }

    public Entry readLatestEntry() {
        Entry lastEntry = new Entry(0, 0);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = br.readLine();
            String[] fragments = line.split(":");
            lastEntry = new Entry(Long.parseLong(fragments[0]), Integer.parseInt(fragments[1]));
            br.close();
        } catch (FileNotFoundException e) {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException ioe) {
                System.out.println(ioe);
                System.out.println("commit log create failed.");
            }
        } catch (IOException e) {
            //log
            System.out.println("read commit log failed.");
        } catch (RuntimeException e) {
            //log
            System.out.println(e);
            System.out.println("parse commit log content failed.");
        }
        return lastEntry;
    }
}
