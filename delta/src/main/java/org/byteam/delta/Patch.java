package org.byteam.delta;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Patch entity.
 * <p>
 * <p>
 * Created by chenenyu on 16/9/14.
 */
class Patch implements Parcelable {
    private String oldPath;
    private String newPath;
    private String patchPath;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.oldPath);
        dest.writeString(this.newPath);
        dest.writeString(this.patchPath);
    }

    public Patch() {
    }

    public Patch(String oldPath, String newPath, String patchPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.patchPath = patchPath;

    }

    protected Patch(Parcel in) {
        this.oldPath = in.readString();
        this.newPath = in.readString();
        this.patchPath = in.readString();
    }

    public static final Parcelable.Creator<Patch> CREATOR = new Parcelable.Creator<Patch>() {
        @Override
        public Patch createFromParcel(Parcel source) {
            return new Patch(source);
        }

        @Override
        public Patch[] newArray(int size) {
            return new Patch[size];
        }
    };

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public String getPatchPath() {
        return patchPath;
    }
}
