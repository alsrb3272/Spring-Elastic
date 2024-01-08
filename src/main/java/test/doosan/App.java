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

import java.nio.charset.Charset;

public class App {
    public static void main( String[] args )
    {
        SpringApplication.run(App.class, args);
        //ip port
        RestClientBuilder builder = RestClient.builder(new HttpHost("180.71.93.106", 9200))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials("elastic", "123456"));
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                });

        RestClient restClient = builder.build();



    Request request = new Request("POST", "/kdrama/_search/template");

        //request body
        /*
        {
            "id": "sample_0001",
            "params": {
              "keyword": "애터미"
            }
          }
        */
//        Gson Agson = new Gson();
        String jsonString = """
                    {
                      "id": "kdrama_0001",
                      "params": {
                        "keyword": "Justice",
                        "field1": "name",
                        "field2": "synopsis"
                      }
                    }
                    """;


/*
        Map<String, Object> map = new HashMap<>();
        map.put("id", "kdrama_0001");

        Map<String, Object> term_1 = new HashMap<>();
        term_1.put("keyword" , "Justice" );

        Map<String, Object> term_2 = new HashMap<>();
        term_2.put( "field1" , "name" );

        Map<String, Object> term_3 = new HashMap<>();
        term_3.put( "field2" , "synopsis" );

        // params 맵 생성
        Map<String, Object> params = new HashMap<>();
        params.putAll(term_1);
        params.putAll(term_2);
        params.putAll(term_3);

        //두개 map 합치기
        map.putAll(params);

        //json string으로 변환
        String mapString = Agson.toJson(map);
*/

        //requeyst body 에 작성한 mapping 싣기
        request.setJsonEntity(jsonString);

        try{

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
            for (int i = 0; i <hitsNum; i++) {

                JsonNode childHit = childHits.get(i);//배열
                JsonNode _source = childHit.get("_source");

                JsonNode name = _source.get("name");
                System.out.println("name = " + name.asText());
                JsonNode synopsis = _source.get("synopsis");
                System.out.println("synopsis = " + synopsis.asText());


                JsonNode highlight = childHit.get("highlight");
                JsonNode test = highlight.get("synopsis");
                for (JsonNode jsonNode2 : test) {
                    System.out.println("synopsis = " + jsonNode2.asText());
                }

            }


        } catch (Exception e) {
            //TODO: handle exception
            System.out.println( "error : "+ e.getMessage() );
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static  String prettyPrinting(String str){

        String result = "";

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(str);
        result = gson.toJson(je);

        return result;

    }
}
