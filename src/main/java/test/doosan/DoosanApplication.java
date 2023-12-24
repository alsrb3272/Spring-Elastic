package test.doosan;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootApplication
public class DoosanApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoosanApplication.class, args);

        RestClientBuilder builder = RestClient.builder(
                        new HttpHost("72b0867a91f4485c99b0e49fbaeafbb0.us-central1.gcp.cloud.es.io", 443, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials("elastic", "123456")); // 여기에 실제 자격증명을 사용하세요.
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });

        RestClient restClient = builder.build();
        createIndexIfNotExists(restClient, "doosan");

        // JSON 파일이 위치한 디렉토리
        String directoryPath = "C:\\Users\\Ethan\\IdeaProjects\\doosan\\src\\main\\resources\\doosan";

        try {
            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> processJsonFile(restClient, "doosan", path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            restClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processJsonFile(RestClient restClient, String indexName, Path filePath) {
        try (Stream<String> stream = Files.lines(filePath)) {
            stream.forEach(jsonData -> {
                try {
                    insertDataIntoElasticsearch(restClient, indexName, jsonData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ElasticSearch에 데이터 삽입
    private static void insertDataIntoElasticsearch(RestClient restClient, String indexName, String jsonData) throws IOException {
        Request request = new Request("POST", "/" + indexName + "/_doc");
        request.setEntity(new NStringEntity(jsonData, ContentType.APPLICATION_JSON));
        Response response = restClient.performRequest(request);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    // 인덱스 생성 요청
    private static void createIndexIfNotExists(RestClient restClient, String indexName) {
        Request request = new Request("HEAD", "/" + indexName);
        try {
            Response response = restClient.performRequest(request);
            if (response.getStatusLine().getStatusCode() == 404) {
                Request createIndexRequest = new Request("PUT", "/" + indexName);
                restClient.performRequest(createIndexRequest);
                System.out.println("Index created: " + indexName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
