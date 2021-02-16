/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.slipkprojects.ultrasshservice.config;

import java.util.UUID;

/**
 * Created by arne on 15.12.16.
 */

public class PasswordCache {
    public static final int AUTHPASSWORD = 3;
    private static PasswordCache mInstance;
	private static UUID mDefaultUuid = UUID.randomUUID();
	
	final private UUID mUuid;
    private String mAuthPassword;

    private PasswordCache(UUID uuid) {
        mUuid = uuid;
    }

    public static PasswordCache getInstance(UUID uuid) {
        if (mInstance == null || !mInstance.mUuid.equals(uuid)) {
            mInstance = new PasswordCache(uuid);
        }
        return mInstance;
    }


    public static String getAuthPassword(UUID uuid, boolean resetPW) {
        if (uuid == null) uuid = mDefaultUuid;
		
		String pwcopy = getInstance(uuid).mAuthPassword;
        if (resetPW)
            getInstance(uuid).mAuthPassword = null;
        return pwcopy;
    }

    public static void setCachedPassword(String uuid, int type, String password) {
        if (uuid == null) uuid = mDefaultUuid.toString();
		
		PasswordCache instance = getInstance(UUID.fromString(uuid));
        switch (type) {
            case AUTHPASSWORD:
                instance.mAuthPassword = password;
                break;
        }
    }


}
