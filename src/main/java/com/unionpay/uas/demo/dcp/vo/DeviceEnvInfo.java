package com.unionpay.uas.demo.dcp.vo;

/**
 * @author lxjiang
 * @date 2023/5/23
 */
public class DeviceEnvInfo {
    private String devId;

    private String devTp;

    private String devLoc;

    private String devModel;

    private String simMobileNo;

    private String simNo;

    private String sourceIpAddr;

    private String macAddr;

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public String getDevTp() {
        return devTp;
    }

    public void setDevTp(String devTp) {
        this.devTp = devTp;
    }

    public String getDevLoc() {
        return devLoc;
    }

    public void setDevLoc(String devLoc) {
        this.devLoc = devLoc;
    }

    public String getDevModel() {
        return devModel;
    }

    public void setDevModel(String devModel) {
        this.devModel = devModel;
    }

    public String getSimMobileNo() {
        return simMobileNo;
    }

    public void setSimMobileNo(String simMobileNo) {
        this.simMobileNo = simMobileNo;
    }

    public String getSimNo() {
        return simNo;
    }

    public void setSimNo(String simNo) {
        this.simNo = simNo;
    }

    public String getSourceIpAddr() {
        return sourceIpAddr;
    }

    public void setSourceIpAddr(String sourceIpAddr) {
        this.sourceIpAddr = sourceIpAddr;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }
}
