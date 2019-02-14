package com.unimas.kstream.bean;

import com.unimas.kstream.JettyServer;

/**
 * 配置的任务信息
 * cpu,mem,runtime通过{@link com.unimas.kstream.web.BackStatusThread}定时更新,
 * 如果页面定时请求会造成页面相应较慢
 * pid：启动jetty时已运行,则启动时获取;jetty运行中启动,则在获取general信息时更新appInfo
 */
public class AppInfo {

    private String name = "";
    private String pid = "";
    private JettyServer.Status status = JettyServer.Status.STOP;
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

    public JettyServer.Status getStatus() {
        return status;
    }

    public void setStatus(JettyServer.Status status) {
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
