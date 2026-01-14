package com.tagokoder.ops;

import org.springframework.boot.SpringApplication;

public class TestOpsApplication {

	public static void main(String[] args) {
		SpringApplication.from(OpsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
