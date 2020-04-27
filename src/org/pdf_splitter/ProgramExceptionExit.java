package org.pdf_splitter;

public class ProgramExceptionExit extends Exception {

    private static final long serialVersionUID = 1L;

    public ProgramExceptionExit(String msg, Throwable t) {
        super(msg, t);
    }

}
