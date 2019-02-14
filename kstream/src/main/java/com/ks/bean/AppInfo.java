package com.ks.bean;


/**
 * 配置的任务信息
 * cpu,mem,runtime通过{@link com.ks.BackStatusThread}定时更新,
 * 如果页面定时请求会造成页面相应较慢
 * pid：启动jetty时已运行,则启动时获取;jetty运行中启动,则在获取general信息时更新appInfo
 */
public class AppInfo {

    public enum Status {
        //运行,启动异常,停止,启动中
        RUN("run"), ODD("odd"), STOP("stop"), START("start");
        private String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    private String name = "";
    private String pid = "";
    private Status status = Status.STOP;
    private String cpu = "—";
    private String mem = "—";
    private String runtime = "—";


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMem() {
        return mem;
    }

    public void setMem(String mem) {
        this.mem = mem;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }
}
