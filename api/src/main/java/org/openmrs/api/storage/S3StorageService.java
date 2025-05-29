package org.openmrs.api.storage;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.openmrs.api.StorageService;
import org.openmrs.api.stream.StreamDataService;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3StorageService extends BaseStorageService implements StorageService {
	
	protected static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
	
	private final DateTimeFormatter keyDateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM-dd/yyyy-MM-dd-HH-mm-ss-SSS-");
	
	private final MimetypesFileTypeMap mimetypes = new MimetypesFileTypeMap();
	
	@Autowired
	private  S3Client s3Client;
	
	String bucketName;
	
	public S3StorageService(@Value("${bucket_name: }") String bucketName, @Autowired StreamDataService streamService){
		super(streamService);
		this.bucketName= StringUtils.isBlank(bucketName)? OpenmrsUtil.getS3ApplicationBucket() :bucketName;
	}
	
	public void createBucket() throws SdkException{
		if (!bucketExsists()) {
			s3Client.createBucket(CreateBucketRequest.builder().bucket(this.bucketName).build());
		}
	}
	
	public void deleteBucket() throws SdkException{
		if (bucketExsists()) {
			s3Client.deleteBucket(req->req.bucket(this.bucketName).build());
		}
	}
	public boolean bucketExsists(){
		try {
			s3Client.headBucket(request -> request.bucket(this.bucketName));
			return true;
		}
		catch (S3Exception exception) {
			return false;
		}
	}
	@Override
	public InputStream getData(String key) throws SdkException {
		GetObjectRequest objectRequest = GetObjectRequest
			.builder()
			.key(getPath(key))
			.bucket(this.bucketName)
			.build();
		return s3Client.getObject(objectRequest, ResponseTransformer.toInputStream());
		
	}
	
	@Override
	public ObjectMetadata getMetadata(String key) throws SdkException {
		HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
			.bucket(this.bucketName)
			.key(getPath(key))
			.build();
		HeadObjectResponse headObjectResponse=s3Client.headObject(headObjectRequest);
		Path path = Paths.get(getPath(key));
		String filename = decodeKey(path.getFileName().toString());
		return ObjectMetadata.builder()
			.setLength(headObjectResponse.contentLength())
			.setMimeType(mimetypes.getContentType(filename))
			.setFilename(filename)
			.setCreationTime(headObjectResponse.lastModified()).build();
		
	}
	
	@Override
	public Stream<String> getKeys(String moduleIdOrGroup, String keyPrefix) throws SdkException {
		String key = newKey(moduleIdOrGroup, keyPrefix, null);
		String target = newPath(key);
		ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
			.bucket(this.bucketName).prefix(target).delimiter("")
			.build();
		ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
		return listObjectsV2Response.contents().stream().map(S3Object::key);
	}
	
	String getPath(String key) {
		return encodeKey(key);
	}
	
	static String decodeKey(String key) {
		try {
			return URLDecoder.decode(key, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	static String encodeKey(String key) {
		try {
			return URLEncoder.encode(key, "UTF-8").replace(".", "%2E")
				.replace("*", "%2A").replace("%2F", "/");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	String newPath(String key) {
		return encodeKey(key);
	}
	

	
	String newKey(String moduleIdOrGroup, String keySuffix, String filename) {
		if (keySuffix == null) {
			keySuffix = LocalDateTime.now().format(keyDateTimeFormat) + RandomStringUtils.insecure().nextAlphanumeric(8);
		}
		if (filename != null) {
			keySuffix += '-' + filename.replace(File.separator, "");
		}
		
		if (moduleIdOrGroup == null) {
			return keySuffix;
		} else {
			if (!moduleIdOrGroupPattern.matcher(moduleIdOrGroup).matches()) {
				throw new IllegalArgumentException("moduleIdOrGroup '" + moduleIdOrGroup + "' does not match [\\w-./]+");
			}
			return moduleIdOrGroup + '/' + keySuffix;
		}
	}
	
	@Override
	public String saveData(InputStream inputStream, ObjectMetadata metadata, String moduleIdOrGroup, String keySuffix)
		throws SdkException,IOException {
		String key = newKey(moduleIdOrGroup, keySuffix, metadata != null ? metadata.getFilename() : null);
		String target = newPath(key);
		try {
			Long contentLength=metadata!=null?metadata.getLength():null;
			String contentType=metadata!=null?metadata.getMimeType():null;
			PutObjectRequest putOb = PutObjectRequest.builder()
				.bucket(this.bucketName)
				.key(target).contentLength(contentLength).contentType(contentType).build();
			String content= IOUtils.toString(inputStream, Charset.defaultCharset());
			PutObjectResponse putObjectResult=s3Client.putObject(putOb,RequestBody.fromString(content));
		}catch(Exception e){
			purgeData(key);
			throw e;
		}
		return key;
	}
	
	@Override
	public boolean purgeData(String key) throws SdkException {
		if (key == null) return false;
		try{
			DeleteObjectRequest del = DeleteObjectRequest.builder()
				.bucket(this.bucketName)
				.key(getPath(key))
				.build();
			DeleteObjectResponse deleteObjectResponse=s3Client.deleteObject(del);
			return true;
		}catch(SdkException e){
			log.error("Error deleting object : {}",key,e);
			return false;
		}
		
	}
	
	@Override
	public boolean exists(String key) {
		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
				.bucket(this.bucketName)
				.key(getPath(key))
				.build();
			s3Client.headObject(headObjectRequest);
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return false;
			} else {
				throw e;
			}
		}
	}
}

