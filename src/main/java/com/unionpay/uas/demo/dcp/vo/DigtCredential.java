package com.unionpay.uas.demo.dcp.vo;

/**
 * @author lxjiang
 * @date 2023/5/23
 */
public class DigtCredential {

    private String StaticCryptData;

    private String CredentialVersion;

    private String Atc;

    private String AcctFaceId;

    private String PriAcctNoMask;

    private String CarrierId;

    private String LocInfoData;

    public String getStaticCryptData() {
        return StaticCryptData;
    }

    public void setStaticCryptData(String staticCryptData) {
        StaticCryptData = staticCryptData;
    }

    public String getCredentialVersion() {
        return CredentialVersion;
    }

    public void setCredentialVersion(String credentialVersion) {
        CredentialVersion = credentialVersion;
    }

    public String getAtc() {
        return Atc;
    }

    public void setAtc(String atc) {
        Atc = atc;
    }

    public String getAcctFaceId() {
        return AcctFaceId;
    }

    public void setAcctFaceId(String acctFaceId) {
        AcctFaceId = acctFaceId;
    }

    public String getPriAcctNoMask() {
        return PriAcctNoMask;
    }

    public void setPriAcctNoMask(String priAcctNoMask) {
        PriAcctNoMask = priAcctNoMask;
    }

    public String getCarrierId() {
        return CarrierId;
    }

    public void setCarrierId(String carrierId) {
        CarrierId = carrierId;
    }

    public String getLocInfoData() {
        return LocInfoData;
    }

    public void setLocInfoData(String locInfoData) {
        LocInfoData = locInfoData;
    }
}
