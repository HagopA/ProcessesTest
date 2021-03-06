import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ProcessOperations {

    private static HashMap<Integer, ProcessInfo> runningProcessesMap;
    private static HashMap<Integer, ProcessInfo> monitoringProcesses;

    public String generateProcessList(){

        this.updateRunningProcessList();
        String text = "=============Processes that are running on the system=============\n";
        text += "=============Scroll down to see the complete list or to go back=============\n\n";

        for(ProcessInfo aProcess : runningProcessesMap.values()){
            text += "Process Name: " + aProcess.getImageName() + "\nPID: " + aProcess.getPid() + "\n\n";
        }
        text += "=============End of list=============";

        return text;
    }

    public boolean addProcessToMonitor(int pid){

        this.updateRunningProcessList();

        if(this.monitoringProcesses != null && this.monitoringProcesses.containsKey(pid) || !this.runningProcessesMap.containsKey(pid)){
            return false;
        }

        String imageName = this.getRunningProcessesMap().get(pid).getImageName();
        String sessionName = this.getRunningProcessesMap().get(pid).getSessionName();
        int sessionNum = this.getRunningProcessesMap().get(pid).getSessionNum();
        int memUsage = this.getRunningProcessesMap().get(pid).getMemUsage();

        if(this.monitoringProcesses == null){
            this.monitoringProcesses = new HashMap<>();
        }
        synchronized (this){
            this.monitoringProcesses.put(pid, new ProcessInfo(imageName, pid, sessionName, sessionNum, memUsage));
        }

        return true;
    }

    public boolean removeProcessToMonitor(int pid){

        if(this.monitoringProcesses != null && this.monitoringProcesses.containsKey(pid)){
            synchronized (this){
                this.monitoringProcesses.remove(pid);
            }
            return true;
        }
        return false;
    }

    public HashMap<Integer, ProcessInfo> updateProcessUsage(){

        for(ProcessInfo aProcess : this.monitoringProcesses.values()){

            ArrayList<String> lines = new ArrayList<>();
            int pid;
            int memUsage;
            int cpuUsage;
            Scanner reader;
            Process processToUpdate;
            Process cpuUsageProcess;
            Scanner cpuUsageReader;

            try{
                processToUpdate = Runtime.getRuntime().exec("tasklist /nh /fi \"pid eq " + aProcess.getPid() + "\"");
                cpuUsageProcess = Runtime.getRuntime().exec("wmic path win32_perfformatteddata_perfproc_" +
                        "process where (IDProcess = '" + aProcess.getPid() + "') get PercentProcessorTime /format:list");
                reader = new Scanner(new InputStreamReader(processToUpdate.getInputStream()));
                cpuUsageReader = new Scanner(new InputStreamReader(cpuUsageProcess.getInputStream()));
            }
            catch(IOException e){
                AlertBox.displayAlertBox("Error!", "Error when processing command.", 250, 400);
                return null;
            }

            String pidCpuUsage = null;
            while(cpuUsageReader.hasNextLine()){
                pidCpuUsage = cpuUsageReader.nextLine();
                if(pidCpuUsage.contains("PercentProcessorTime=")){
                    break;
                }
            }

            int i = 0;
            int indexOf;

            while(reader.hasNextLine()) {

                lines.add(reader.nextLine());

                if(lines.get(i).length() > 1){
                    indexOf = lines.get(i).indexOf("  ");
                    lines.set(i, lines.get(i).substring(indexOf).trim());

                    indexOf = lines.get(i).indexOf(" ");
                    pid = Integer.parseInt(lines.get(i).substring(0, indexOf));
                    lines.set(i, lines.get(i).substring(indexOf).trim());

                    indexOf = lines.get(i).indexOf("  ");
                    lines.set(i, lines.get(i).substring(indexOf).trim());

                    indexOf = lines.get(i).indexOf("  ");
                    lines.set(i, lines.get(i).substring(indexOf).trim());

                    indexOf = lines.get(i).indexOf(" ");
                    memUsage = Integer.parseInt(lines.get(i).substring(0, indexOf).replace(",", ""));
                    lines.set(i, lines.get(i).substring(indexOf).trim());

                    cpuUsage = Integer.parseInt(pidCpuUsage.substring(pidCpuUsage.indexOf("=")+1));

                    this.monitoringProcesses.get(pid).setMemUsage(memUsage);
                    this.monitoringProcesses.get(pid).setPercentCpuUsage(cpuUsage);
                }
                i++;
            }
            cpuUsageReader.close();
            reader.close();
        }
        return monitoringProcesses;
    }

    private void updateRunningProcessList(){

        this.runningProcessesMap = new HashMap<>();
        ArrayList<String> lines = new ArrayList<>();
        Process runningProcesses;
        Scanner reader;
        String imageName;
        int pid;
        String sessionName;
        int sessionNum;
        int memUsage;

        try{
            runningProcesses = Runtime.getRuntime().exec("tasklist /nh");
            reader = new Scanner(new InputStreamReader(runningProcesses.getInputStream()));
        }
        catch(IOException e){
            AlertBox.displayAlertBox("Error!", "Error when processing command.", 250, 400);
            return;
        }

        for(int i = 0, indexOf; reader.hasNextLine(); i++){
            lines.add(reader.nextLine());

            if(lines.get(i).length() != 0){
                indexOf = lines.get(i).indexOf("  ");
                imageName = lines.get(i).substring(0, indexOf);
                lines.set(i, lines.get(i).substring(indexOf).trim());

                indexOf = lines.get(i).indexOf(" ");
                pid = Integer.parseInt(lines.get(i).substring(0, indexOf));
                lines.set(i, lines.get(i).substring(indexOf).trim());

                indexOf = lines.get(i).indexOf("  ");
                sessionName = lines.get(i).substring(0, indexOf);
                lines.set(i, lines.get(i).substring(indexOf).trim());

                indexOf = lines.get(i).indexOf("  ");
                sessionNum = Integer.parseInt(lines.get(i).substring(0, indexOf));
                lines.set(i, lines.get(i).substring(indexOf).trim());

                indexOf = lines.get(i).indexOf(" ");
                memUsage = Integer.parseInt(lines.get(i).substring(0, indexOf).replace(",", ""));
                lines.set(i, lines.get(i).substring(indexOf).trim());

                this.runningProcessesMap.put(pid, new ProcessInfo(imageName, pid, sessionName, sessionNum, memUsage));
            }
        }
        reader.close();
    }

    public static HashMap<Integer, ProcessInfo> getRunningProcessesMap() {
        return runningProcessesMap;
    }

    public static HashMap<Integer, ProcessInfo> getMonitoringProcesses() {
        return monitoringProcesses;
    }
}
