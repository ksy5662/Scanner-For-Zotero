package org.ale.scan2zotero.web;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;


public class GoogleBooksAPIClient {

    public static final String BOOK_SEARCH_ISBN = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private Handler mHandler;

    public GoogleBooksAPIClient(Handler handler) {
        mHandler = handler;
        mHttpsClient = HttpsClient.getHttpsClientInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    public void isbnLookup(String isbn) {
        APIRequest g = new APIRequest(mHandler, mHttpsClient);
        g.setRequestType(APIRequest.GET);
        g.setURI(URI.create(BOOK_SEARCH_ISBN+isbn));

        mRequestQueue.enqueue(g);
    }
}
