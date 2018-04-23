package com.unimas.kstream.bean;


public class AppInfo {

    public enum Status {
        //运行,启动异常,停止,启动中,未部署(数据库数据未生成配置文件或配置文件不是最新数据)
        RUN("run"), ODD("odd"), STOP("stop"), START("start"), INIT("init");
        private String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private String name = "";
    private String desc = "";
    private String pid = "";
    private Status status = Status.INIT;
    private String cpu = "—";
    private String mem = "—";
    private String runtime = "—";


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
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

    public void setStatus(String status) {
        if (Status.RUN.getValue().equals(status)) this.status = Status.RUN;
        else if (Status.ODD.getValue().equals(status)) this.status = Status.ODD;
        else if (Status.STOP.getValue().equals(status)) this.status = Status.STOP;
        else this.status = Status.INIT;
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
