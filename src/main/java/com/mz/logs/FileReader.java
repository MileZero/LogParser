package com.mz.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;

public class FileReader {
    public static void main(String[] args) throws Exception {
        (new LogParser()).parseFile("/work/MZ/logs/test.log");
    }
}
