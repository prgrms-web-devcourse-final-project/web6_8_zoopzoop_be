package org.tuna.zoopzoop.backend.global.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일 업로드 메서드
     * @param multipartFile 업로드할 파일
     * @param fileName S3에 저장될 파일 이름
     * @return 업로드된 파일의 URL 주소
     * @throws IOException 파일 처리 중 발생할 수 있는 예외
     */
    public String upload(MultipartFile multipartFile, String fileName) throws IOException {
        // 1. PutObjectRequest 객체 생성 (빌더 패턴 사용)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(multipartFile.getContentType())
                .build();

        // 2. S3에 파일 업로드 (InputStream을 직접 사용)
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        // 3. 업로드된 파일의 URL 주소 반환
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(fileName)).toString();
    }

    /**
     * S3에 파일 업로드 (byte[])💡
     * @param bytes 업로드할 파일의 바이트 배열
     * @param fileName S3에 저장될 파일 이름
     * @param contentType 파일의 MIME 타입 (e.g., "image/png")
     * @return 업로드된 파일의 URL
     */
    public String upload(byte[] bytes, String fileName, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(fileName)).toString();
    }

    /**
     * S3에서 파일 삭제 메서드
     * @param fileName 삭제할 파일 이름
     */
    public void delete(String fileName) {
        s3Client.deleteObject(builder -> builder.bucket(bucket).key(fileName).build());
    }
}
