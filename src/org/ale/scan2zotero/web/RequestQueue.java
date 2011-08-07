package org.ale.scan2zotero.web;

import java.util.ArrayList;
import java.util.LinkedList;

public class RequestQueue {

    private static final int CAPACITY = 5;
    
    private static RequestQueue instance;
    
    private static ArrayList<APIRequest> active =  new ArrayList<APIRequest>();

    private static LinkedList<APIRequest> queue = new LinkedList<APIRequest>();

    public static RequestQueue getInstance(){
        if(instance == null)
            instance = new RequestQueue();
        return instance;
    }

    public synchronized void enqueue(APIRequest task){
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

    public synchronized void taskComplete(APIRequest task){
        active.remove(task);
        startNext();
    }
}
