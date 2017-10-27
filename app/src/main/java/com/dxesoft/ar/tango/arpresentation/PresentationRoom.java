package com.dxesoft.ar.tango.arpresentation;

/**
 * Created by wangxigang on 2017/10/26.
 */

public class PresentationRoom {
    private String mRoomName;
    private int mRoomID;
    private String mRoomAddress;

    PresentationRoom(){
        mRoomName = "";
        mRoomID = getRoomID();
        mRoomAddress = "";
    }

    private int getRoomID(){
        return 100;
    }
}
