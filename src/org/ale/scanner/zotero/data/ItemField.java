/** 
 * Copyright 2011 John M. Schanck
 * 
 * ScannerForZotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ScannerForZotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ScannerForZotero.  If not, see <http://www.gnu.org/licenses/>.
 */

// XXX: This is temporary, ultimately we should fetch all ItemFields
// through api.zotero.org/itemFields?locale=xxx, and store the result in a db

package org.ale.scanner.zotero.data;

import java.util.HashMap;

public final class ItemField {
    public static final String creators = "creators";
    public static final String itemType = "itemType";
    public static final String notes = "notes";
    public static final String tags = "tags";

    public final class Creator {
        public static final String firstName = "firstName";
        public static final String lastName = "lastName";
        public static final String name = "name";
    }

    public final class Note {
        public static final String note = "note";
        public static final String itemType = "itemType";
    }

    public final class Tag {
        public static final String tag = "tag";
        public static final String type = "type";
    }

    public static final String numPages = "numPages";
    public static final String numberOfVolumes = "numberOfVolumes";
    public static final String abstractNote = "abstractNote";
    public static final String accessDate = "accessDate";
    public static final String applicationNumber = "applicationNumber";
    public static final String archive = "archive";
    public static final String artworkSize = "artworkSize";
    public static final String assignee = "assignee";
    public static final String billNumber = "billNumber";
    public static final String blogTitle = "blogTitle";
    public static final String bookTitle = "bookTitle";
    public static final String callNumber = "callNumber";
    public static final String caseName = "caseName";
    public static final String code = "code";
    public static final String codeNumber = "codeNumber";
    public static final String codePages = "codePages";
    public static final String codeVolume = "codeVolume";
    public static final String committee = "committee";
    public static final String company = "company";
    public static final String conferenceName = "conferenceName";
    public static final String country = "country";
    public static final String court = "court";
    public static final String DOI = "DOI";
    public static final String date = "date";
    public static final String dateDecided = "dateDecided";
    public static final String dateEnacted = "dateEnacted";
    public static final String dictionaryTitle = "dictionaryTitle";
    public static final String distributor = "distributor";
    public static final String docketNumber = "docketNumber";
    public static final String documentNumber = "documentNumber";
    public static final String edition = "edition";
    public static final String encyclopediaTitle = "encyclopediaTitle";
    public static final String episodeNumber = "episodeNumber";
    public static final String extra = "extra";
    public static final String audioFileType = "audioFileType";
    public static final String filingDate = "filingDate";
    public static final String firstPage = "firstPage";
    public static final String audioRecordingFormat = "audioRecordingFormat";
    public static final String videoRecordingFormat = "videoRecordingFormat";
    public static final String forumTitle = "forumTitle";
    public static final String genre = "genre";
    public static final String history = "history";
    public static final String ISBN = "ISBN";
    public static final String ISSN = "ISSN";
    public static final String institution = "institution";
    public static final String issue = "issue";
    public static final String issueDate = "issueDate";
    public static final String issuingAuthority = "issuingAuthority";
    public static final String journalAbbreviation = "journalAbbreviation";
    public static final String label = "label";
    public static final String language = "language";
    public static final String programmingLanguage = "programmingLanguage";
    public static final String legalStatus = "legalStatus";
    public static final String legislativeBody = "legislativeBody";
    public static final String libraryCatalog = "libraryCatalog";
    public static final String archiveLocation = "archiveLocation";
    public static final String interviewMedium = "interviewMedium";
    public static final String artworkMedium = "artworkMedium";
    public static final String meetingName = "meetingName";
    public static final String nameOfAct = "nameOfAct";
    public static final String network = "network";
    public static final String pages = "pages";
    public static final String patentNumber = "patentNumber";
    public static final String place = "place";
    public static final String postType = "postType";
    public static final String priorityNumbers = "priorityNumbers";
    public static final String proceedingsTitle = "proceedingsTitle";
    public static final String programTitle = "programTitle";
    public static final String publicLawNumber = "publicLawNumber";
    public static final String publicationTitle = "publicationTitle";
    public static final String publisher = "publisher";
    public static final String references = "references";
    public static final String reportNumber = "reportNumber";
    public static final String reportType = "reportType";
    public static final String reporter = "reporter";
    public static final String reporterVolume = "reporterVolume";
    public static final String rights = "rights";
    public static final String runningTime = "runningTime";
    public static final String scale = "scale";
    public static final String section = "section";
    public static final String series = "series";
    public static final String seriesNumber = "seriesNumber";
    public static final String seriesText = "seriesText";
    public static final String seriesTitle = "seriesTitle";
    public static final String session = "session";
    public static final String shortTitle = "shortTitle";
    public static final String studio = "studio";
    public static final String subject = "subject";
    public static final String system = "system";
    public static final String title = "title";
    public static final String thesisType = "thesisType";
    public static final String mapType = "mapType";
    public static final String manuscriptType = "manuscriptType";
    public static final String letterType = "letterType";
    public static final String presentationType = "presentationType";
    public static final String url = "url";
    public static final String university = "university";
    public static final String version = "version";
    public static final String volume = "volume";
    public static final String websiteTitle = "websiteTitle";
    public static final String websiteType = "websiteType";
    
    public static final HashMap<String, String> Localized;
    static {
        Localized = new HashMap<String, String>();

        // Item Type
        Localized.put("itemType", "Item Type");

        // Item Fields
        Localized.put("numPages","# of Pages");
        Localized.put("numberOfVolumes","# of Volumes");
        Localized.put("abstractNote","Abstract");
        Localized.put("accessDate","Accessed");
        Localized.put("applicationNumber","Application Number");
        Localized.put("archive","Archive");
        Localized.put("artworkSize","Artwork Size");
        Localized.put("assignee","Assignee");
        Localized.put("billNumber","Bill Number");
        Localized.put("blogTitle","Blog Title");
        Localized.put("bookTitle","Book Title");
        Localized.put("callNumber","Call Number");
        Localized.put("caseName","Case Name");
        Localized.put("code","Code");
        Localized.put("codeNumber","Code Number");
        Localized.put("codePages","Code Pages");
        Localized.put("codeVolume","Code Volume");
        Localized.put("committee","Committee");
        Localized.put("company","Company");
        Localized.put("conferenceName","Conference Name");
        Localized.put("country","Country");
        Localized.put("court","Court");
        Localized.put("DOI","DOI");
        Localized.put("date","Date");
        Localized.put("dateDecided","Date Decided");
        Localized.put("dateEnacted","Date Enacted");
        Localized.put("dictionaryTitle","Dictionary Title");
        Localized.put("distributor","Distributor");
        Localized.put("docketNumber","Docket Number");
        Localized.put("documentNumber","Document Number");
        Localized.put("edition","Edition");
        Localized.put("encyclopediaTitle","Encyclopedia Title");
        Localized.put("episodeNumber","Episode Number");
        Localized.put("extra","Extra");
        Localized.put("audioFileType","File Type");
        Localized.put("filingDate","Filing Date");
        Localized.put("firstPage","First Page");
        Localized.put("audioRecordingFormat","Format");
        Localized.put("videoRecordingFormat","Format");
        Localized.put("forumTitle","Forum Title");
        Localized.put("genre","Genre");
        Localized.put("history","History");
        Localized.put("ISBN","ISBN");
        Localized.put("ISSN","ISSN");
        Localized.put("institution","Institution");
        Localized.put("issue","Issue");
        Localized.put("issueDate","Issue Date");
        Localized.put("issuingAuthority","Issuing Authority");
        Localized.put("journalAbbreviation","Journal Abbr");
        Localized.put("label","Label");
        Localized.put("language","Language");
        Localized.put("programmingLanguage","Language");
        Localized.put("legalStatus","Legal Status");
        Localized.put("legislativeBody","Legislative Body");
        Localized.put("libraryCatalog","Library Catalog");
        Localized.put("archiveLocation","Loc. in Archive");
        Localized.put("interviewMedium","Medium");
        Localized.put("artworkMedium","Medium");
        Localized.put("meetingName","Meeting Name");
        Localized.put("nameOfAct","Name of Act");
        Localized.put("network","Network");
        Localized.put("pages","Pages");
        Localized.put("patentNumber","Patent Number");
        Localized.put("place","Place");
        Localized.put("postType","Post Type");
        Localized.put("priorityNumbers","Priority Numbers");
        Localized.put("proceedingsTitle","Proceedings Title");
        Localized.put("programTitle","Program Title");
        Localized.put("publicLawNumber","Public Law Number");
        Localized.put("publicationTitle","Publication");
        Localized.put("publisher","Publisher");
        Localized.put("references","References");
        Localized.put("reportNumber","Report Number");
        Localized.put("reportType","Report Type");
        Localized.put("reporter","Reporter");
        Localized.put("reporterVolume","Reporter Volume");
        Localized.put("rights","Rights");
        Localized.put("runningTime","Running Time");
        Localized.put("scale","Scale");
        Localized.put("section","Section");
        Localized.put("series","Series");
        Localized.put("seriesNumber","Series Number");
        Localized.put("seriesText","Series Text");
        Localized.put("seriesTitle","Series Title");
        Localized.put("session","Session");
        Localized.put("shortTitle","Short Title");
        Localized.put("studio","Studio");
        Localized.put("subject","Subject");
        Localized.put("system","System");
        Localized.put("title","Title");
        Localized.put("thesisType","Type");
        Localized.put("mapType","Type");
        Localized.put("manuscriptType","Type");
        Localized.put("letterType","Type");
        Localized.put("presentationType","Type");
        Localized.put("url","URL");
        Localized.put("university","University");
        Localized.put("version","Version");
        Localized.put("volume","Volume");
        Localized.put("websiteTitle","Website Title");
        Localized.put("websiteType","Website Type");
        
        // Creator fields
        Localized.put("name", "Name");
        Localized.put("firstName", "First");
        Localized.put("lastName", "Last");

        // Notes and Tags fields
        Localized.put("note", "Note");
        Localized.put("notes", "Notes");
        Localized.put("tag", "Tag");
        Localized.put("tags", "Tags");
    }

}