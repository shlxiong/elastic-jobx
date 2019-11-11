package com.openxsl.elasticjob;

import java.util.Properties;

import com.openxsl.config.EnvironmentLoader;
import com.openxsl.config.loader.DomainPropertyLoader;

public class JobPropertiesLoader implements DomainPropertyLoader{

	@Override
	public Properties loadProperties() {
		return EnvironmentLoader.load("", "elastic.job", "job");
	}

}
