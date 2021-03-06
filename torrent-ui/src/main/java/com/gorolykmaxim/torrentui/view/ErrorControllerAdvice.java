package com.gorolykmaxim.torrentui.view;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
public class ErrorControllerAdvice {

    @ExceptionHandler(ViewError.class)
    public ModelAndView showError(Throwable e) {
        ModelAndView modelAndView = new ModelAndView("error");
        applyErrorInformationTo(modelAndView, e);
        return modelAndView;
    }

    public void applyErrorInformationTo(ModelAndView modelAndView, Throwable e) {
        StringWriter errorStackTraceWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(errorStackTraceWriter);
        e.printStackTrace(printWriter);
        modelAndView.addObject("error", e.getMessage());
        modelAndView.addObject("stackTrace", errorStackTraceWriter.toString());
    }
}
