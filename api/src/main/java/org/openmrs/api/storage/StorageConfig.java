package org.openmrs.api.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class StorageConfig {
	
	/**
	 * @return an instance of S3Client
	 */
	@Bean
	public static S3Client s3Client() {
		return S3Client.builder()
			.httpClientBuilder(ApacheHttpClient.builder()).region(Region.of("us-east-1"))
			.build();
	}
}
