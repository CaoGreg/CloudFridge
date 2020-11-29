package com.cloudfridge;

import android.graphics.drawable.Drawable;

public class ExampleItem {
    private Drawable imageResource;
    private String text1;
    private String text2;
    private String date;

    public ExampleItem(Drawable imageResource, String text1, String text2, String date) {
        this.imageResource = imageResource;
        this.text1 = text1;
        this.text2 = text2;
        this.date = date;
    }

    public Drawable getImageResource() {
        return this.imageResource;
    }

    public String getText1() {
        return this.text1;
    }

    public String getText2() {
        return this.text2;
    }

    public String getDate() {
        return this.date;
    }
}
