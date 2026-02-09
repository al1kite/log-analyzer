package com.electricip.loganalyzer.domain.exception;

/**
 * 업로드 파일이 허용 크기를 초과했을 때 발생하는 예외
 */
public class FileTooLargeException extends InvalidFileException {

    private final long fileSizeBytes;
    private final long maxSizeBytes;

    public FileTooLargeException(String message, long fileSizeBytes, long maxSizeBytes) {
        super("FILE_TOO_LARGE", message);
        this.fileSizeBytes = fileSizeBytes;
        this.maxSizeBytes = maxSizeBytes;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }
}
