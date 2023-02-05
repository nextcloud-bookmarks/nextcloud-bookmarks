package org.schabi.ocbookmarks.REST;

import java.io.IOException;

/**
 *
 * Todo: Phase out the String message and try to use enum.
 * Reason: I think it makes handling errors more easy because we dont rely on string parsing.
 * Created by the-scrabi on 14.05.17.
 */
public class RequestException extends IOException {


    public enum ERROR {
        UNKNOWN,
        API_NOT_SET_UP,
        FILE_NOT_FOUND,
        HOST_NOT_FOUND,
        TIME_OUT,
        BOOKMARK_NOT_INSTALLED
    }

    private ERROR mError = ERROR.UNKNOWN;


    @Deprecated
    RequestException(String message, Exception e) {
        super(message, e);
    }

    RequestException(Exception e) {
        super(e);
    }


    @Deprecated
    RequestException(String message) {
        super(message);
    }

    @Deprecated
    RequestException(String message, ERROR error) {
        super(message);
        mError = error;
    }

    RequestException(ERROR error) {
        super(error.name());
        mError = error;
    }

    public ERROR getError() {
        return mError;
    }
}
