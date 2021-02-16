package com.slipkprojects.ultrasshservice.logger;

import android.os.Parcel;
import android.os.Parcelable;

public enum ConnectionStatus implements Parcelable {
    LEVEL_CONNECTED,
    LEVEL_CONNECTING_SERVER_REPLIED,
    LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
    LEVEL_NONETWORK,
    LEVEL_NOTCONNECTED,
    LEVEL_START,
    LEVEL_AUTH_FAILED,
    LEVEL_WAITING_FOR_USER_INPUT,
    UNKNOWN_LEVEL;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ConnectionStatus> CREATOR = new Creator<ConnectionStatus>() {
        @Override
        public ConnectionStatus createFromParcel(Parcel in) {
            return ConnectionStatus.values()[in.readInt()];
        }

        @Override
        public ConnectionStatus[] newArray(int size) {
            return new ConnectionStatus[size];
        }
    };
}

