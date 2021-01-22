package com.oursky.authgeartest.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.oursky.authgeartest.MainApplication;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
    private static final String TAG = WXEntryActivity.class.getSimpleName();

    private IWXAPI api;

    private static OnWeChatSendAuthResultListener onWeChatSendAuthResultListener;

    public static void setOnWeChatSendAuthResultListener(OnWeChatSendAuthResultListener listener) {
        onWeChatSendAuthResultListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, MainApplication.AUTHGEAR_WECHAT_APP_ID, false);
        try {
            Intent intent = getIntent();
            api.handleIntent(intent, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
        Log.d(TAG, "onReq: " + req.getType());
        finish();
    }

    @Override
    public void onResp(BaseResp resp) {
        String result = null;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = "errcode_cancel";
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = "errcode_deny";
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                result = "errcode_unsupported";
                break;
            default:
                result = "errcode_unknown";
                break;
        }

        if (result != null) {
            Log.e(TAG, "WeChat login error: " + result);
        }

        if (resp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            SendAuth.Resp authResp = (SendAuth.Resp)resp;
            if (onWeChatSendAuthResultListener != null) {
                onWeChatSendAuthResultListener.OnResult(authResp.code, authResp.state);
            }
        }
        finish();
    }

    public interface OnWeChatSendAuthResultListener{
        void OnResult(String code, String state);
    }
}
