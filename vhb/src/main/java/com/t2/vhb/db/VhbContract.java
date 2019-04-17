/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * VirtualHopeBox
 *
 * Copyright  2009-2014 United States Government as represented by
 * the Chief Information Officer of the National Center for Telehealth
 * and Technology. All Rights Reserved.
 *
 * Copyright  2009-2014 Contributors. All Rights Reserved.
 *
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE,
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY").
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES,
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 *
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: VirtualHopeBox001
 * Government Agency Original Software Title: VirtualHopeBox
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.vhb.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Virtual Hope Box content provider and its
 * clients.
 * 
 * @author wes
 */
public class VhbContract {

    public static final String AUTHORITY = "com.t2.vhb";

    private static final String SCHEME = "content://";

    private static final Uri BASE_URI = Uri.parse(SCHEME + AUTHORITY);

    private static final String DIR_MIME_TYPE_BASE = "vnd.android.cursor.dir";

    private static final String ITEM_MIME_TYPE_BASE = "vnd.android.cursor.item";


    public static final class YouTube implements BaseColumns {

        public static final String TABLE_NAME = "youtube";

        public static final String PATH = TABLE_NAME;

        public static final String PATH_FOR_ID = PATH + "/#";
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.youtube";

        public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.youtube";

        public static final String COL_URL = "url";
        public static final String COL_THUMBNAIL = "thumbnail";

        private YouTube() {
        }
    }

    public static final class Media implements BaseColumns {
        public static final String TABLE_NAME = "media";

        public static final String PATH = TABLE_NAME;

        public static final String COL_EXTERNAL_ID = "external_id";

        public static final String COL_MEDIA_TYPE = "media_type";

        public static final String COL_INACTIVE = "inactive";

        public static final String COL_ROTATION = "rotation";

        public static final String COL_FILE_PATH = "file_path";

        public static final String COL_REMOTE_ONLY = "remote_only";

        public static final String COL_LOCAL_THUMBNAIL_PATH = "local_thumbnail";

        public static final String COL_LOCAL_TITLE = "title";

        public static final String PATH_FOR_TYPE = PATH + "/*";

        public static final String PATH_FOR_TYPE_AND_EXTERNAL_ID = PATH_FOR_TYPE + "/ext/#";

        public static final String PATH_FOR_TYPE_AND_ID = PATH_FOR_TYPE + "/#";
        public static final int MEDIA_TYPE_POSITION = 1;
        public static final int MEDIA_EXTERNAL_ID_POSITION = 3;
        public static final int MEDIA_ID_POSITION = 2;

        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();

        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.media";

        public static String getContentMimeType(String mediaType) {
            switch (mediaType) {
                case Photos.MEDIA_TYPE:
                    return Photos.CONTENT_MIME_TYPE;
                case Videos.MEDIA_TYPE:
                    return Videos.CONTENT_MIME_TYPE;
                case Messages.MEDIA_TYPE:
                    return Messages.CONTENT_MIME_TYPE;
                case Music.MEDIA_TYPE:
                    return Music.CONTENT_MIME_TYPE;
                default:
                    throw new IllegalArgumentException("Unsupported Media Type: " + mediaType);
            }
        }

        public static Uri getContentUri(String mediaType) {
            switch (mediaType) {
                case Photos.MEDIA_TYPE:
                    return Photos.CONTENT_URI;
                case Videos.MEDIA_TYPE:
                    return Videos.CONTENT_URI;
                case Messages.MEDIA_TYPE:
                    return Messages.CONTENT_URI;
                case Music.MEDIA_TYPE:
                    return Music.CONTENT_URI;
                case BreathingMusic.MEDIA_TYPE:
                    return BreathingMusic.CONTENT_URI;
                default:
                    throw new IllegalArgumentException("Unsupported Media Type: " + mediaType);
            }
        }

        public static String getItemContentMimeType(String mediaType) {
            switch (mediaType) {
                case Photos.MEDIA_TYPE:
                    return Photos.CONTENT_ITEM_MIME_TYPE;
                case Videos.MEDIA_TYPE:
                    return Videos.CONTENT_ITEM_MIME_TYPE;
                case Messages.MEDIA_TYPE:
                    return Messages.CONTENT_ITEM_MIME_TYPE;
                case Music.MEDIA_TYPE:
                    return Music.CONTENT_ITEM_MIME_TYPE;
                case BreathingMusic.MEDIA_TYPE:
                    return BreathingMusic.CONTENT_ITEM_MIME_TYPE;
                default:
                    throw new IllegalArgumentException("Unsupported Media Type: " + mediaType);
            }
        }

        private Media() {
        }

        public static final class Messages {
            public static final String MEDIA_TYPE = "message";

            public static final String PATH = Media.PATH + "/" + MEDIA_TYPE;

            public static final String PATH_FOR_ID = PATH + "/#";
            public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.message";

            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.message";

            private Messages() {
            }
        }

        public static final class Music {
            public static final String MEDIA_TYPE = "music";

            public static final String PATH = Media.PATH + "/" + MEDIA_TYPE;
            public static final String PATH_FOR_ID = PATH + "/#";

            public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.music";
            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.music";
        }

        public static final class BreathingMusic {
            public static final String MEDIA_TYPE = "breathing_music";

            public static final String PATH = Media.PATH + "/" + MEDIA_TYPE;
            public static final String PATH_FOR_ID = PATH + "/#";

            public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.breathing_music";
            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.breathing_music";
        }

        public static final class Photos {
            public static final String MEDIA_TYPE = "photo";

            public static final String PATH = Media.PATH + "/" + MEDIA_TYPE;
            public static final String PATH_FOR_ID = PATH + "/#";

            public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.photo";
            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.photo";
        }

        public static final class Videos {
            public static final String MEDIA_TYPE = "video";

            public static final String PATH = Media.PATH + "/" + MEDIA_TYPE;
            public static final String PATH_FOR_ID = PATH + "/#";

            public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.video";
            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.video";
        }

        public static final String[] MEDIA_TYPES = {
                Messages.MEDIA_TYPE, Music.MEDIA_TYPE, BreathingMusic.MEDIA_TYPE, Photos.MEDIA_TYPE, Videos.MEDIA_TYPE
        };
    }

    public static final class Quotes implements BaseColumns {
        public static final String TABLE_NAME = "quotes";

        /**
         * 0-relative position of the quote ID segment of the URI
         */
        public static final int QUOTE_ID_POSITION = 1;

        public static final int QUOTE_AUTHOR_POSITION = 1;

        public static final String PATH = TABLE_NAME;
        public static final String PATH_FOR_AUTHOR = PATH + "/*";

        public static final String PATH_FOR_ID = PATH + "/#";
        /**
         * URI to obtain a list of quotes
         */
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();
        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.quote";

        public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.quote";

        public static final String COL_AUTHOR = "author";
        public static final String COL_QUOTE = "quote";

        public static final String COL_FAVORITE = "favorite";

        public static final String COL_CATEGORY = "category";

        private Quotes() {
        }

        public static Uri getContentUri(long quoteId) {
            return Quotes.CONTENT_URI.buildUpon().appendEncodedPath(quoteId + "").build();
        }

        public enum QuoteCategory {
            QUOTE("Quotes"), JOKE("Jokes"), RELIGIOUS_TEXT("Religious Text");
            private final String mName;

            QuoteCategory(String name) {
                mName = name;
            }

            public String getName() {
                return mName;
            }
        }
    }

    public static final class ActivityIdea implements BaseColumns {
        public static final String TABLE_NAME = "activity_ideas";
        public static final String COL_NAME = "name";
        public static final String COL_VERB = "verb";
        public static final String COL_FAVORITE = "favorite";

        public static final String PATH = TABLE_NAME;

        public static final String PATH_FOR_ID = PATH + "/#";
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendEncodedPath(PATH).build();
        public static final int ACTIVITY_IDEA_ID_POSITION = 1;
        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.activity_idea";

        public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.activity_idea";

        private ActivityIdea() {
        }
    }

    /**
     * Support Contact table contract
     * 
     * @author wes
     */
    public static final class SupportContacts implements BaseColumns {
        public static final String TABLE_NAME = "support_contacts";

        /**
         * 0-relative position of the support contact ID segment of the URI
         */
        public static final int SUPPORT_CONTACT_ID_POSITION = 1;

        public static final int SUPPORT_CONTACT_LOOKUP_KEY_POSITION = 1;
        public static final String PATH = TABLE_NAME;

        public static final String PATH_FOR_ID = PATH + "/#";
        public static final String PATH_FOR_LOOKUP = PATH + "/*";
        /**
         * URI to obtain a list of support contacts
         */
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();

        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE + "/vnd.t2.vhb.support_contact";

        public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE + "/vnd.t2.vhb.support_contact";
        public static final String COL_LOOKUP_KEY = "lookup_key";

        public static final String COL_CONTACT_ID = "contact_id";

        private SupportContacts() {
        }
    }

}
