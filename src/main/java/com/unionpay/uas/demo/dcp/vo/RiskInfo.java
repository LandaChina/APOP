package com.unionpay.uas.demo.dcp.vo;

/**
 * @author lxjiang
 * @date 2023/5/23
 */
public class RiskInfo {

    private String riskScore;

    private String riskStandardVersion;

    private String deviceScore;

    private String accountScore;

    private String phoneNumberScore;

    private String riskReasonCode;

    private String applyChannel;

    private String captureMethod;

    private String goodsTp;

    public String getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(String riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskStandardVersion() {
        return riskStandardVersion;
    }

    public void setRiskStandardVersion(String riskStandardVersion) {
        this.riskStandardVersion = riskStandardVersion;
    }

    public String getDeviceScore() {
        return deviceScore;
    }

    public void setDeviceScore(String deviceScore) {
        this.deviceScore = deviceScore;
    }

    public String getAccountScore() {
        return accountScore;
    }

    public void setAccountScore(String accountScore) {
        this.accountScore = accountScore;
    }

    public String getPhoneNumberScore() {
        return phoneNumberScore;
    }

    public void setPhoneNumberScore(String phoneNumberScore) {
        this.phoneNumberScore = phoneNumberScore;
    }

    public String getRiskReasonCode() {
        return riskReasonCode;
    }

    public void setRiskReasonCode(String riskReasonCode) {
        this.riskReasonCode = riskReasonCode;
    }

    public String getApplyChannel() {
        return applyChannel;
    }

    public void setApplyChannel(String applyChannel) {
        this.applyChannel = applyChannel;
    }

    public String getCaptureMethod() {
        return captureMethod;
    }

    public void setCaptureMethod(String captureMethod) {
        this.captureMethod = captureMethod;
    }

    public String getGoodsTp() {
        return goodsTp;
    }

    public void setGoodsTp(String goodsTp) {
        this.goodsTp = goodsTp;
    }
}
