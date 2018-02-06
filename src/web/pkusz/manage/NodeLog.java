package web.pkusz.manage;

import java.io.*;

/**
 * Created by nick on 2018/2/3.
 */
public class NodeLog {

    // file path: log/n(node)-1(id)-0(file seq).
    public static String LOG_FILE_PATH = "log/";
    public static String NODELOG_PRIFIX = "n";

    public static String assembleFilePath(String id) {
        StringBuilder s = new StringBuilder();
        s.append(LOG_FILE_PATH);
        s.append(NODELOG_PRIFIX);
        s.append("-");
        s.append(id);
        s.append("-0");
        return s.toString();
    }

    String id;
    File file;

    public NodeLog(String id) {
        this.id = id;
        String fp = assembleFilePath(id);
        File logFile = new File(fp);
        if (!logFile.exists()) {
            try {
                if(!logFile.createNewFile())
                    //log
                    throw new RuntimeException("node log create failed. id:" + id);
            } catch (IOException e) {
                //log
                System.out.println(e);
                throw new RuntimeException("node log create failed. id:" + id);
            }
        }
        file = logFile;
    }

    public boolean appendRecord(String r) {
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            pw.println(r);
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException ioe) {
                System.out.println(ioe);
                System.out.println("node log create failed. id:" + id);
            }
            return false;
        }
        return true;
    }

    public void printAll() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException ioe) {
                System.out.println(ioe);
                System.out.println("node log create failed. id:" + id);
            }
        } catch (IOException e) {
            //log
            System.out.println("read node log failed. id:" + id);
        } catch (RuntimeException e) {
            //log
            System.out.println(e);
            System.out.println("parse node log content failed. id:" + id);
        }
    }
}
