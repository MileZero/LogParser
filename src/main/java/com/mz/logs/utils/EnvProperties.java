package com.mz.logs.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvProperties {
    private String environment;
    private String allServices;
    private List<String> allServicesPath;
    private String grayLogUrl;
}
