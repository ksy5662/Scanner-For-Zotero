package org.ale.scan2zotero.web.zotero;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ale.scan2zotero.data.Access;
import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.Group;
import org.ale.scan2zotero.web.APIRequest;
import org.ale.scan2zotero.web.HttpsClient;
import org.ale.scan2zotero.web.RequestQueue;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class ZoteroAPIClient {
    //private static final String CLASS_TAG = ZoteroAPIClient.class.getCanonicalName();

    public static final int COLLECTIONS = 0;
    public static final int GROUPS = 1;
    public static final int ITEMS = 2;
    public static final int PERMISSIONS = 3;

    public static final String EXTRA_REQ_TYPE = "RT";
    public static final String EXTRA_ITEM_ROWS = "ROWS";

    private static final String ZOTERO_BASE_URL = "https://api.zotero.org";
    private static final String ZOTERO_USERS_URL = ZOTERO_BASE_URL + "/users";
    private static final String ZOTERO_GROUPS_URL = ZOTERO_BASE_URL + "/groups";

    private static final String HDR_WRITE_TOKEN = "X-Zotero-Write-Token";

    private Account mAccount;

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private ZoteroHandler mHandler;

    public ZoteroAPIClient() {
        mHandler = ZoteroHandler.getInstance();
        mHttpsClient = HttpsClient.getInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    public void setAccount(Account acct){
        mAccount = acct;
    }

    public APIRequest newRequest(){
        return new APIRequest(mHandler, mHttpsClient);
    }

    public void addItems(JSONObject items, int[] rows, int userOrGroupId) {
        // https://apis.zotero.org/users/<userid>/items

        // TODO: Allow no more than 50 items at a time
        APIRequest r = newRequest();
        r.setHttpMethod(APIRequest.POST);
        HashMap<String, String> queryTerms = new HashMap<String,String>();
        queryTerms.put("content", "json");
        queryTerms.put("key", mAccount.getKey());
        r.setURI(buildURI(queryTerms, String.valueOf(userOrGroupId), "items"));
        r.setContent(items.toString(), "application/json");
        r.addHeader(HDR_WRITE_TOKEN, newWriteToken());
        Bundle extra = new Bundle();
        extra.putIntArray(EXTRA_ITEM_ROWS, rows);
        extra.putInt(EXTRA_REQ_TYPE, ZoteroAPIClient.ITEMS);
        r.setExtra(extra);

        mRequestQueue.enqueue(r);
    }

    public void getPermissions() {
        // https://apis.zotero.org/users/<userid>/keys/<apikey>
        APIRequest r = newRequest();
        r.setHttpMethod(APIRequest.GET);
        r.setURI(buildURI(null, mAccount.getUid(), "keys", mAccount.getKey()));
        Bundle extra = new Bundle();
        extra.putInt(EXTRA_REQ_TYPE, ZoteroAPIClient.PERMISSIONS);
        r.setExtra(extra);
        mRequestQueue.enqueue(r);
    }

    public void getGroups() {
        // https://apis.zotero.org/users/<userid>/groups
        APIRequest r = newRequest();
        r.setHttpMethod(APIRequest.GET);
        r.setURI(buildURI(null, mAccount.getUid(), "groups"));
        Bundle extra = new Bundle();
        extra.putInt(EXTRA_REQ_TYPE, ZoteroAPIClient.GROUPS);
        r.setExtra(extra);

        mRequestQueue.enqueue(r);
    }

    public void newCollection(String name, String parent){
        // https://apis.zotero.org/users/<userid>/collections
        JSONObject collection = new JSONObject();

        try {
            collection.put("name", name);
            collection.put("parent", parent);
            APIRequest r = newRequest();
            r.setHttpMethod(APIRequest.POST);
            r.setURI(buildURI(null, mAccount.getUid(), "collections"));
            r.setContent(collection.toString(), "application/json");
            r.addHeader(HDR_WRITE_TOKEN, newWriteToken());
            Bundle extra = new Bundle();
            extra.putInt(EXTRA_REQ_TYPE, ZoteroAPIClient.COLLECTIONS);
            r.setExtra(extra);

            mRequestQueue.enqueue(r);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public URI buildURI(Map<String, String> queryTerms, String...pathSections) {
        // Returns:
        // https://api.zotero.org/<user or group>/<persona>/<action>?key=<key>
        String base;
        StringBuilder queryB = new StringBuilder();
        if(queryTerms == null){
            queryB.append("key=").append(mAccount.getKey());
        }else{
            for(Entry<String, String> e : queryTerms.entrySet()){
                queryB.append(e.getKey())
                    .append("=")
                    .append(e.getValue())
                    .append("&");
            }
            // Delete the last ampersand
            queryB.deleteCharAt(queryB.length()-1);
        }

        String persona = pathSections[0];
        if(persona.equals(mAccount.getUid())){
            base = ZOTERO_USERS_URL;
        }else{
            base = ZOTERO_GROUPS_URL;
        }
        String path = TextUtils.join("/", pathSections);
        return URI.create(base+"/"+path+"?"+queryB.toString());
    }

    public static String newWriteToken(){
        // Make 16 hex character write token
        Random rng = new Random();
        return Integer.toHexString(rng.nextInt()) + Integer.toHexString(rng.nextInt());
    }

    public static Access parsePermissions(String resp, Account user) {
        /* example:
          <key key="xxx">
          <access library="1" files="1" notes="1" write="1"/>
          <access group="12345" write="1"/>
          <access group="all" write="1"/>
          </key>
         */
        Log.d("APIClient", resp);
        Document doc = ZoteroAPIClient.parseXML(resp);
        if(doc == null) return null;

        NodeList keys = doc.getElementsByTagName("key");
        if(keys.getLength() == 0) return null;

        Node keyNode = keys.item(0);
        Node keyAttr = keyNode.getAttributes().getNamedItem("key");
        if(keyAttr == null) return null;

        String key = keyAttr.getNodeValue();
        if(!key.equals(user.getKey())) return null;

        NodeList accessTags = doc.getElementsByTagName("access");
        int[] groups = new int[accessTags.getLength()];
        int[] permissions = new int[accessTags.getLength()];
        for(int i=0; i<accessTags.getLength(); i++){
            permissions[i] = Access.READ;

            NamedNodeMap attr = accessTags.item(i).getAttributes();
            Node groupNode = attr.getNamedItem("group");
            if(groupNode == null){ // Library access?
                groupNode = attr.getNamedItem("library");
                if(groupNode == null)
                    return null;
                groups[i] = Group.GROUP_LIBRARY;
            }else{ // Individual group or all groups
                if(groupNode.getNodeValue().equals("all"))
                    groups[i] = Group.GROUP_ALL;
                else
                    groups[i] = Integer.parseInt(groupNode.getNodeValue());
            }

            Node writeNode = attr.getNamedItem("write");
            if(writeNode != null && writeNode.getNodeValue().equals("1")){
                permissions[i] |= Access.WRITE;
            }

            Node noteNode = attr.getNamedItem("notes");
            if(noteNode != null && noteNode.getNodeValue().equals("1")){
                permissions[i] |= Access.NOTE;
            }
        }
        return new Access(user.getDbId(), groups, permissions);
    }

    public static Group[] parseGroups(String resp) {
        /* example:
          <!-- tons of garbage -->
            <zapi:totalResults>1</zapi:totalResults>
            <zapi:apiVersion>1</zapi:apiVersion>
            <updated>2011-07-15T22:46:21Z</updated>
            <entry xmlns:zxfer="http://zotero.org/ns/transfer">
                <title>group title</title>
                <author>
                  <name>some_user</name>
                  <uri>http://zotero.org/some_user</uri>
                </author>
                <id>http://zotero.org/groups/12345</id>
                <published>2011-07-15T22:46:01Z</published>
                <updated>2011-07-15T22:46:21Z</updated>
                <link rel="self" type="application/atom+xml" href="https://api.zotero.org/groups/12345"/>
                <link rel="alternate" type="text/html" href="http://zotero.org/groups/12345"/>
                <zapi:numItems>10</zapi:numItems>
                <content type="html">
                <!-- more garbage -->
                </content>
            </entry>
         */

        /* Returns null for parsing errors */
        Document doc = ZoteroAPIClient.parseXML(resp);
        if(doc == null)
            return null;

        NodeList totalResults = doc.getElementsByTagName("zapi:totalResults");
        if(totalResults.getLength() == 0)
            return null;

        String trStr = totalResults.item(0).getTextContent();
        if(trStr == null || !TextUtils.isDigitsOnly(trStr))
            return null;

        int numGroups = Integer.parseInt(trStr);
        Group[] groups = new Group[numGroups];

        NodeList entries = doc.getElementsByTagName("entry");
        if(entries.getLength() != numGroups)
            return null;

        for(int i=0; i<numGroups; i++){
            if(entries.item(i).getNodeType() != Node.ELEMENT_NODE)
                return null;
            NodeList titles = ((Element) entries.item(i)).getElementsByTagName("title");
            NodeList ids = ((Element) entries.item(i)).getElementsByTagName("id");
            if(titles.getLength() != 1 || ids.getLength() != 1)
                return null;
            String title = titles.item(0).getTextContent();
            String idUri = ids.item(0).getTextContent();
            if(title == null || idUri == null)
                return null;
            int lastSeg = idUri.lastIndexOf("/");
            if(lastSeg < 0)
                return null;
            String idstr = idUri.substring(lastSeg+1);
            if(!TextUtils.isDigitsOnly(idstr))
                return null;
            int id = Integer.parseInt(idstr);
            groups[i] = new Group(id, title);
        }

        return groups;
    }

    public static String parseItems(String resp) {
        /* example:
          <!-- tons of garbage -->
          <zapi:totalResults>1</zapi:totalResults>
          <zapi:apiVersion>1</zapi:apiVersion>
          <updated>2010-12-17T00:18:51Z</updated>
            <entry>
              <title>My Book</title>
              <author>
                  <name>Zotero User</name>
                  <uri>http://zotero.org/zuser</uri>
              </author>
              <id>http://zotero.org/users/zuser/items/ABCD2345</id>
              <published>2010-12-17T00:18:51Z</published>
              <updated>2010-12-17T00:18:51Z</updated>
              <link rel="self" type="application/atom+xml" href="https://api.zotero.org/users/1/items/ABCD2345?content=json"/>
              <link rel="alternate" type="text/html" href="http://zotero.org/users/zuser/items/ABCD2345"/>
              <zapi:key>ABCD2345</zapi:key>
              <zapi:itemType>book</zapi:itemType>
              <zapi:creatorSummary>McAuthor</zapi:creatorSummary>
              <zapi:numChildren>1</zapi:numChildren>
              <zapi:numTags>2</zapi:numTags>
              <content type="application/json" etag="8e984e9b2a8fb560b0085b40f6c2c2b7">
                {
                  "itemType" : "book",
                  "title" : "My Book",
                  "creators" : [
                    {
                      "creatorType" : "author",
                      "firstName" : "Sam",
                      "lastName" : "McAuthor"
                    },
                    {
                      "creatorType":"editor",
                      "name" : "John T. Singlefield"
                    }
                  ],
                  "tags" : [
                    { "tag" : "awesome" },
                    { "tag" : "rad", "type" : 1 }
                  ]
                }
              </content>
            </entry>
        </feed>
         */

        /* Returns null for parsing errors */
        Document doc = ZoteroAPIClient.parseXML(resp);
        if(doc == null)
            return null;

        NodeList totalResults = doc.getElementsByTagName("zapi:totalResults");
        if(totalResults.getLength() == 0)
            return null;

        String trStr = totalResults.item(0).getTextContent();
        if(trStr == null || !TextUtils.isDigitsOnly(trStr))
            return null;

        int numGroups = Integer.parseInt(trStr);
        Group[] groups = new Group[numGroups];

        NodeList entries = doc.getElementsByTagName("entry");
        if(entries.getLength() != numGroups)
            return null;

        for(int i=0; i<numGroups; i++){
            if(entries.item(i).getNodeType() != Node.ELEMENT_NODE)
                return null;
            NodeList titles = ((Element) entries.item(i)).getElementsByTagName("title");
            NodeList ids = ((Element) entries.item(i)).getElementsByTagName("id");
            if(titles.getLength() != 1 || ids.getLength() != 1)
                return null;
            String title = titles.item(0).getTextContent();
            String idUri = ids.item(0).getTextContent();
            if(title == null || idUri == null)
                return null;
            int lastSeg = idUri.lastIndexOf("/");
            if(lastSeg < 0)
                return null;
            String idstr = idUri.substring(lastSeg+1);
            if(!TextUtils.isDigitsOnly(idstr))
                return null;
            int id = Integer.parseInt(idstr);
            groups[i] = new Group(id, title);
        }

        return "";
    }

    public static Document parseXML(String xml){
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            ByteArrayInputStream encXML = new ByteArrayInputStream(xml.getBytes("UTF8"));
            doc = builder.parse(encXML);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}
