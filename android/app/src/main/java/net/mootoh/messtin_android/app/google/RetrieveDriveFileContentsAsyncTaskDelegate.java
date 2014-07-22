package net.mootoh.messtin_android.app.google;

public interface RetrieveDriveFileContentsAsyncTaskDelegate {
    public void onError(RetrieveDriveFileContentsAsyncTask task, Error error);
    public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result);
}
