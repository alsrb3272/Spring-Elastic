package test.doosan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class App  {

    public static void main(String[] args) {

        // Start the Spring application context
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);

        // Obtain the PropertyConfig bean from the application context
        PropertyConfig propertyConfig = context.getBean(PropertyConfig.class);


        String hostname = propertyConfig.getHostname();
        int port = propertyConfig.getPort();
        String username = propertyConfig.getUsername();
        String password = propertyConfig.getPassword();

        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });

        RestClient restClient = builder.build();


        Request request = new Request("POST", "/kdrama/_search/template");

        //request body
        /*
        String jsonString = """
            {
              "id": "kdrama_0001",
              "params": {
                "keyword": "Justice",
                "field1": "name",
                "field2": "synopsis"
              }
            }
        """;*/

        ObjectMapper objectMapper1 = new ObjectMapper();

        // JSON 구조에 맞는 Map 생성
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("keyword", "Justice");
        paramsMap.put("field1", "name");
        paramsMap.put("field2", "synopsis");
        // paramsMap.put("size", "25,50,100"); # 사이트에서 보고 싶은 항목의 개수를 선택 25,50,100 픽
        jsonMap.put("id", "kdrama_0001");
        jsonMap.put("params", paramsMap);

        try {
            // Map을 JSON 문자열로 변환
            String jsonString = objectMapper1.writeValueAsString(jsonMap);

            // Request 객체에 JSON 본문 설정
            request.setJsonEntity(jsonString);

            Response response = restClient.performRequest(request);

            //반환값 httpEntity -> string 변환하여 전달 (바로 json 으로 바꿀경우 문제발생  -> " <- 이스케이프 문제   )
            String responseBody = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
            //System.out.println("response --> " + responseBody)


            clearScreen();
            //string -> json 으로 가져오기
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(responseBody, JsonNode.class);

            //문자로 json  key 로 value 가져오기
            JsonNode brandNode = jsonNode.get("took");
            String took = brandNode.asText();
            System.out.println("took = " + took);

            //hits -> hits
            JsonNode hits = jsonNode.get("hits");
            JsonNode childHits = hits.get("hits");
            int hitsNum = childHits.size();

            //return value
            /*
                "_source": {
                    "id": "238756802",
                    "name": "애터미 헤모힘*1set"
                },
                "fields": {
                    "id": [
                        "238756802"
                    ]
                },
                "highlight": {
                    "name.ngram": [
                        "<em>애터미 </em>헤모힘*1set"
                    ]
            */

            //반복되어야 하는 부분
            for (int i = 0; i < hitsNum; i++) {

                JsonNode childHit = childHits.get(i); // 배열
                JsonNode _source = childHit.get("_source");

                JsonNode name = _source.get("name");
                System.out.println("name = " + name.asText());
                JsonNode synopsis = _source.get("synopsis");
                if (synopsis != null) { // synopsis가 null이 아닌 경우에만 출력
                    System.out.println("synopsis = " + synopsis.asText());
                }

                JsonNode highlight = childHit.get("highlight");
                if (highlight != null) {
                    JsonNode synopsisHighlights = highlight.get("synopsis");

                    // 하이라이트된 'synopsis' 필드의 모든 요소를 결합
                    if (synopsisHighlights != null) {
                        StringBuilder synopsisBuilder = new StringBuilder();
                        for (JsonNode snippet : synopsisHighlights) {
                            synopsisBuilder.append(snippet.asText()).append(" ");
                        }
                        // 결합된 하이라이트를 출력
                        String combinedSynopsis = synopsisBuilder.toString().trim();
                        System.out.println("Combined synopsis highlight: " + combinedSynopsis);

                    }
                }
            }
        } catch (Exception e) {
            //TODO: handle exception
            System.out.println("error : " + e.getMessage());

        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String prettyPrinting(String str) {

        String result = "";

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(str);
        result = gson.toJson(je);

        return result;

    }
}
