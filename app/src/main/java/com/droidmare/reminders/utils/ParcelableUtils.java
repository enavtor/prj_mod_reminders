package com.droidmare.reminders.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Convert between Parcelable object and byte[]
 * @author enavas on 14/09/2017.
 */

public class ParcelableUtils{

    /**
     * Converts from Parcelable to byte[]
     * @param parcelable Origin
     * @return Conversion to byte[]
     */
    public static byte[] marshall(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    /**
     * Converts Convert from byte[] to T parcelable object
     * @param bytes Origin
     * @param creator Creator of parcelable object
     * @param <T> Class of parcelable object
     * @return T object
     */
    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel=unmarshall(bytes);
        T result=creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }

    /**
     * Convert from byte[] to Parcel
     * @param bytes Origin
     * @return Parcel object
     */
    private static Parcel unmarshall(byte[] bytes){
        Parcel parcel=Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // This is extremely important!
        return parcel;
    }
}
