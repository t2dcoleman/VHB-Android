/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * CopingCardLib
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
 * Government Agency Original Software Designation: CopingCardLib001
 * Government Agency Original Software Title: CopingCardLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.copingcards;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Virtual Hope Box content provider and its
 * clients.
 * 
 * @author wes
 */
public class CopingContract {

    public static final String AUTHORITY = "com.t2.copingcards";

    private static final String SCHEME = "content://";

    private static final Uri BASE_URI = Uri.parse(SCHEME + AUTHORITY);

    private static final String DIR_MIME_TYPE_BASE = "vnd.android.cursor.dir";

    private static final String ITEM_MIME_TYPE_BASE = "vnd.android.cursor.item";

    public static final class CopingCard implements BaseColumns {
        public static final String TABLE_NAME = "coping_cards";

        /**
         * 0-relative position of the coping card ID segment of the URI
         */
        public static final int COPING_CARD_ID_POSITION = 1;

        public static final String PATH = TABLE_NAME;

        public static final String PATH_FOR_ID = PATH + "/#";
        /**
         * URI to obtain a list of coping cards
         */
        public static final Uri CONTENT_URI = BASE_URI.buildUpon()
                .appendPath(PATH).build();

        public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE
                + "/vnd.t2.vhb.coping_card";

        public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE
                + "/vnd.t2.vhb.coping_card";

        public static final String COL_PROBLEM_AREA = "problem_area";

        public static Uri getContentUri(long copingCardId) {
            return CopingCard.CONTENT_URI.buildUpon()
                    .appendEncodedPath(copingCardId + "")
                    .build();
        }

        private CopingCard() {
        }

        public static final class Symptom implements BaseColumns {
            public static final String TABLE_NAME = "symptom";

            /**
             * 0-relative position of the positive belief ID segment of the URI
             */
            public static final int COPING_SYMPTOM_POSITION = 3;

            public static final String PATH = CopingCard.PATH_FOR_ID + "/"
                    + TABLE_NAME;

            public static final String PATH_FOR_ID = PATH + "/#";

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE
                    + "/vnd.t2.vhb.symptom";

            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE
                    + "/vnd.t2.vhb.symptom";

            public static final String COL_SYMPTOM_TEXT = "symptom";
            public static final String COL_COPING_CARD_ID = "coping_card_id";

            public static Uri getContentUri(long copingCardId) {
                return CopingCard.CONTENT_URI.buildUpon()
                        .appendEncodedPath(copingCardId + "")
                        .appendEncodedPath(TABLE_NAME).build();
            }

            private Symptom() {
            }
        }

        public static final class CopingSkill implements BaseColumns {
            public static final String TABLE_NAME = "coping_skill";

            /**
             * 0-relative position of the positive belief ID segment of the URI
             */
            public static final int COPING_SKILL_ID_POSITION = 3;

            public static final String PATH = CopingCard.PATH_FOR_ID + "/"
                    + TABLE_NAME;

            public static final String PATH_FOR_ID = PATH + "/#";

            public static final String CONTENT_MIME_TYPE = DIR_MIME_TYPE_BASE
                    + "/vnd.t2.vhb.coping_skill";

            public static final String CONTENT_ITEM_MIME_TYPE = ITEM_MIME_TYPE_BASE
                    + "/vnd.t2.vhb.coping_skill";

            public static final String COL_SKILL_TEXT = "skill";
            public static final String COL_COPING_CARD_ID = "coping_card_id";

            public static Uri getContentUri(long copingCardId) {
                return CopingCard.CONTENT_URI.buildUpon()
                        .appendEncodedPath(copingCardId + "")
                        .appendEncodedPath(TABLE_NAME).build();
            }

            private CopingSkill() {
            }
        }
    }

}
