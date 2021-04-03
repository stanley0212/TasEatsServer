package com.luvtas.taseatsserver.Model;

import java.util.List;

public class FCMResponse {
    private long multicase_id;
    private int success, failure, canonical_ids;
    private List<FCMResult> resultList;
    private long message_id;

    public long getMulticase_id() {
        return multicase_id;
    }

    public void setMulticase_id(long multicase_id) {
        this.multicase_id = multicase_id;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailure() {
        return failure;
    }

    public void setFailure(int failure) {
        this.failure = failure;
    }

    public int getCanonical_ids() {
        return canonical_ids;
    }

    public void setCanonical_ids(int canonical_ids) {
        this.canonical_ids = canonical_ids;
    }

    public List<FCMResult> getResultList() {
        return resultList;
    }

    public void setResultList(List<FCMResult> resultList) {
        this.resultList = resultList;
    }

    public long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(long message_id) {
        this.message_id = message_id;
    }
}
