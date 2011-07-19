package org.ale.scan2zotero.web;

import java.util.ArrayList;
import java.util.LinkedList;

public class RequestQueue {

    private static final int CAPACITY = 5;
    
    private static RequestQueue instance;
    
    private ArrayList<APIRequest> active =  new ArrayList<APIRequest>();

    private LinkedList<APIRequest> queue = new LinkedList<APIRequest>();

    public static RequestQueue getInstance(){
        if(instance == null)
            instance = new RequestQueue();
        return instance;
    }

    protected void enqueue(APIRequest task){
        queue.add(task);
        if(active.size() < CAPACITY){
            startNext();
        }
    }

    private void startNext() {
        if(!queue.isEmpty()){
            APIRequest next = queue.poll();
            active.add(next);
            Thread task = new Thread(next);
            task.start();
        }
    }

    private void taskComplete(APIRequest task){
        active.remove(task);
        startNext();
    }
}
