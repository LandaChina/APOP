package com.unionpay.uas.demo.dcp.vo;

/**
 * @author lxjiang
 * @date 2023/5/23
 */
public class AcctInfo {

    private String priAcctNo;

    private String cardTp;

    private String contactNm;

    private String contactNmHash;

    private String contactMobileNo;

    private String contactMobileNoMask;

    private String certifTp;

    private String certifId;

    private String certIdHash;

    private String acctUserId;

    private String acctFaceId;


    public String getPriAcctNo() {
        return priAcctNo;
    }

    public void setPriAcctNo(String priAcctNo) {
        this.priAcctNo = priAcctNo;
    }

    public String getCardTp() {
        return cardTp;
    }

    public void setCardTp(String cardTp) {
        this.cardTp = cardTp;
    }

    public String getContactNm() {
        return contactNm;
    }

    public void setContactNm(String contactNm) {
        this.contactNm = contactNm;
    }

    public String getContactNmHash() {
        return contactNmHash;
    }

    public void setContactNmHash(String contactNmHash) {
        this.contactNmHash = contactNmHash;
    }

    public String getContactMobileNo() {
        return contactMobileNo;
    }

    public void setContactMobileNo(String contactMobileNo) {
        this.contactMobileNo = contactMobileNo;
    }

    public String getContactMobileNoMask() {
        return contactMobileNoMask;
    }

    public void setContactMobileNoMask(String contactMobileNoMask) {
        this.contactMobileNoMask = contactMobileNoMask;
    }

    public String getCertifTp() {
        return certifTp;
    }

    public void setCertifTp(String certifTp) {
        this.certifTp = certifTp;
    }

    public String getCertifId() {
        return certifId;
    }

    public void setCertifId(String certifId) {
        this.certifId = certifId;
    }

    public String getCertIdHash() {
        return certIdHash;
    }

    public void setCertIdHash(String certIdHash) {
        this.certIdHash = certIdHash;
    }

    public String getAcctUserId() {
        return acctUserId;
    }

    public void setAcctUserId(String acctUserId) {
        this.acctUserId = acctUserId;
    }

    public String getAcctFaceId() {
        return acctFaceId;
    }

    public void setAcctFaceId(String acctFaceId) {
        this.acctFaceId = acctFaceId;
    }
}
