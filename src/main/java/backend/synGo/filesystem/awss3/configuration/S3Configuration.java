package backend.synGo.filesystem.awss3.configuration;

import backend.synGo.filesystem.FileStorageManager;
import backend.synGo.filesystem.awss3.CloudfrontCacheInvalidator;
import backend.synGo.filesystem.awss3.S3StorageManager;
import backend.synGo.filesystem.awss3.S3UrlConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Configuration {

    @Value("${aws.s3.region}")
    private String s3Region;

    @Value("${aws.credentials.access-key-id}")
    private String accessKey;

    @Value("${aws.credentials.secret-access-key}")
    private String secretKey;

    @Value("${aws.cloud-front.region}")
    private String cloudFrontRegion;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public FileStorageManager fileCloudUploader() {
        return new S3StorageManager(
                s3Client(),
                cloudfrontCacheInvalidator(),
                new S3UrlConverter()
        );
    }

    @Bean
    public CloudfrontCacheInvalidator cloudfrontCacheInvalidator() {
        return new CloudfrontCacheInvalidator(
                CloudFrontClient.builder()
                        .region(Region.of(cloudFrontRegion))
                        .build()
        );
    }
}