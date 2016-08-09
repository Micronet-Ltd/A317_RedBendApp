package com.redbend.app;

public class SmmException extends Exception {

    private static final long serialVersionUID = -5706957665089300782L;
    
    SmmException () {
    }

    public SmmException (String message) {
        super (message);
    }

    public SmmException (Throwable cause) {
        super (cause);
    }

    public SmmException (String message, Throwable cause) {
        super (message, cause);
    }
    
}
