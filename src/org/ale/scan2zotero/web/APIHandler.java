package org.ale.scan2zotero.web;

import java.util.ArrayList;

import org.ale.scan2zotero.S2ZMainActivity;
import org.ale.scan2zotero.web.APIRequest.APIResponse;
import org.apache.http.StatusLine;

import android.os.Handler;
import android.os.Message;

public abstract class APIHandler extends Handler {

    public static final String CLASS_TAG = APIHandler.class.getCanonicalName();

    protected static ArrayList<Integer> mResponseTypes = new ArrayList<Integer>();
    protected static ArrayList<APIResponse> mResponses = new ArrayList<APIResponse>();

    protected static S2ZMainActivity mActivity = null;

    protected abstract void onStart(String id);
    protected abstract void onProgress(String id, int percent);
    protected abstract void onFailure(String id, StatusLine reason);
    protected abstract void onException(String id, Exception exc);
    protected abstract void onSuccess(String id, String res);

    public void registerActivity(S2ZMainActivity activity){
        if(mActivity != null || activity == null)
            return;
        mActivity = activity;
        for(int i=0; i<mResponses.size(); i++){
            handleMessage(mResponseTypes.get(i).intValue(), mResponses.get(i));
        }
        mResponseTypes.clear();
        mResponses.clear();
    }

    public void unregisterActivity(){
        mActivity = null;
    }

    public void handleMessage(Message msg){
        APIResponse resp = (APIResponse)msg.obj;

        if(mActivity == null){
            mResponseTypes.add(new Integer(msg.what));
            mResponses.add(resp);
        }else{
            handleMessage(msg.what, resp);
        }
    }

    public void handleMessage(int type, APIResponse resp){
        String id = resp.getId();

        switch(type) {
        case APIRequest.START:
            onStart(id);
            break;
        case APIRequest.PROGRESS:
            onProgress(id, ((Integer)resp.getData()).intValue());
            break;
        case APIRequest.EXCEPTION:
            onException(id, (Exception)resp.getData());
            break;
        case APIRequest.FAILURE:
            onFailure(id, (StatusLine)resp.getData());
            break;
        case APIRequest.SUCCESS:
            onSuccess(id, (String) resp.getData());
            break;
        case APIRequest.FINISH:
            RequestQueue.getInstance().taskComplete((APIRequest)resp.getData());
            break;
        }
    }
}
