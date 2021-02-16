/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.slipkprojects.ultrasshservice.aidl;

/**
 * Created by arne on 15.11.16.
 */

interface IUltraSSHServiceInternal {

    /**
     * @param replaceConnection True if the VPN is connected by a new connection.
     * @return true if there was a process that has been send a stop signal
     */
    void stopVPN();
}
