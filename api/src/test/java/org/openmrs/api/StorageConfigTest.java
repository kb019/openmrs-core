package org.openmrs.api;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import java.net.URI;

import com.adobe.testing.s3mock.S3MockApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.utils.AttributeMap;

@Configuration
public class StorageConfigTest {

	@Bean
	S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of("us-west-1"))
			.credentialsProvider(
				StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy_access_key", "dummy_secret_access_key")))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.endpointOverride(URI.create("http://localhost:"+S3MockApplication.DEFAULT_HTTP_PORT))
			.httpClient(ApacheHttpClient.builder().buildWithDefaults(AttributeMap.builder()
				.put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
				.build()))
			.build();
	}

}
