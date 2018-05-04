package com.unimas.kstream.bean;

import java.util.HashMap;
import java.util.Map;

public class ServiceInfo {

    private String name;
    private String desc;
    private Map<String, AppInfo> appInfoMap;

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

    public Map<String, AppInfo> getAppInfoMap() {
        return appInfoMap;
    }

    public void setAppInfoMap(Map<String, AppInfo> appInfoMap) {
        this.appInfoMap = appInfoMap;
    }

    public void addAppInfo(AppInfo appInfo) {
        if (this.appInfoMap == null) this.appInfoMap = new HashMap<>(5);
        this.appInfoMap.put(appInfo.getId(), appInfo);
    }
}
