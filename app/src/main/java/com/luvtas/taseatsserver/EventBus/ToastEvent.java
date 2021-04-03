package com.luvtas.taseatsserver.EventBus;

import com.luvtas.taseatsserver.Common.Common;

public class ToastEvent {
//    private boolean isUpdate;
//    private boolean isFromFoodList;
//
//    public ToastEvent(boolean isUpdate, boolean isFromFoodList) {
//        this.isUpdate = isUpdate;
//        this.isFromFoodList = isFromFoodList;
//    }
//
//    public boolean isUpdate() {
//        return isUpdate;
//    }
//
//    public void setUpdate(boolean update) {
//        isUpdate = update;
//    }
//
//    public boolean isFromFoodList() {
//        return isFromFoodList;
//    }
//
//    public void setFromFoodList(boolean fromFoodList) {
//        isFromFoodList = fromFoodList;
//    }

    private Common.ACTION action;
    private boolean isFromFoodList;

    public ToastEvent(Common.ACTION action, boolean isFromFoodList) {
        this.action = action;
        this.isFromFoodList = isFromFoodList;
    }

    public Common.ACTION getAction() {
        return action;
    }

    public void setAction(Common.ACTION action) {
        this.action = action;
    }

    public boolean isFromFoodList() {
        return isFromFoodList;
    }

    public void setFromFoodList(boolean fromFoodList) {
        isFromFoodList = fromFoodList;
    }
}
