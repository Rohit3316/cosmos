package org.cosmos.s3;

public final class S3Constants {

    public static final String S3_CLIENT_ERROR_MSG = "S3 client must not be null";
    public static final String FILE_UPLOAD_ERROR_MSG = "File upload must not be null";
    public static final String BUCKET_NAME_ERROR_MSG = "Bucket Name must not be null or empty";
    public static final String KEY_PATH_ERROR_MSG = "File key path must not be null or empty";
    public static final String FILE_NAME_ERROR_MSG = "File name must not be null or empty";
    public static final String FILE_PATH_ERROR_MSG = "File path must not be null or empty";

    private S3Constants() {
        // constant class, private constructor.
    }
}
