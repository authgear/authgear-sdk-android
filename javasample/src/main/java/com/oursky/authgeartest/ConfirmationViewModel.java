package com.oursky.authgeartest;

class ConfirmationViewModel {

    public ConfirmationViewModel(String message) {
        this.message = message;
    }
    public String message = "";
    public void onConfirm() {}
    public void onCancel() {}
}
