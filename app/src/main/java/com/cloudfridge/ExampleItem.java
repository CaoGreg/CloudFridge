package com.cloudfridge;

public class ExampleItem {
    private int imageResource;
    private String text1;
    private String text2;

    public ExampleItem(int imageResource, String text1, String text2) {
        this.imageResource = imageResource;
        this.text1 = text1;
        this.text2 = text2;
    }

    public int getImageResource() {
        return this.imageResource;
    }

    public String getText1() {
        return this.text1;
    }

    public String getText2() {
        return this.text2;
    }
}
