package net.mootoh.messtin_android.app;

public abstract class BookStorage {
    public abstract void retrieve(Book book, String path, OnImageRetrieved callback);

    protected String filenameForPage(int page) {
        String name = "%03d.jpg";
        name = String.format(name, page);
        return name;
    }


}
