package com.joyowo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class SpringbootcasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootcasApplication.class, args);
	}

}
