package com.unimas.kstream.bean;


public class AppInfo {

    public enum Status {
        //运行,启动异常,停止,启动中,未部署(数据库数据未生成配置文件或配置文件不是最新数据)
        RUN("run", 4), ODD("odd", 3), STOP("stop", 1),
        START("start", 2), INIT("init", 0);
        private String value;
        private int type;

        Status(String value, int type) {
            this.value = value;
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public int getType() {
            return type;
        }
    }

    private String id = "";
    private String name = "";
    private String desc = "";
    private String pid = "";
    private Status status = Status.INIT;
    private String cpu = "—";
    private String mem = "—";
    private String runtime = "—";
    private String zkUrl = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public void setStatus(int status) {
        if (Status.RUN.getType() == status) this.status = Status.RUN;
        else if (Status.ODD.getType() == status) this.status = Status.ODD;
        else if (Status.STOP.getType() == status) this.status = Status.STOP;
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

    public String getZkUrl() {
        return zkUrl;
    }

    public void setZkUrl(String zkUrl) {
        this.zkUrl = zkUrl;
    }
}
