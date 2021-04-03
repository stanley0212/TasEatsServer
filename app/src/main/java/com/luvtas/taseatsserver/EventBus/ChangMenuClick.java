package com.luvtas.taseatsserver.EventBus;

public class ChangMenuClick {
    private boolean isFromFoodList;

    public ChangMenuClick(boolean isFromFoodList) {
        this.isFromFoodList = isFromFoodList;
    }

    public boolean isFromFoodList() {
        return isFromFoodList;
    }

    public void setFromFoodList(boolean fromFoodList) {
        isFromFoodList = fromFoodList;
    }
}
