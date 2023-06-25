package com.veera;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ExternalApiService {

    private String mainJsonToken;
    @Autowired
    private RestTemplate restTemplate;

    ExecutorService executorService = Executors.newFixedThreadPool(32);

    @Autowired
    private CSVUtil csvUtil;

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private AppConfig appConfig;

    @PostConstruct
    public void processApi() {
        try {
            //String token = getToken();
            //callContactIdApi(token);
            //downloadAudio(token);
            downloadAudioThreadPool();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Step 1: Get the authentication token
     */
    private String getToken() {

        RequestEntity<String> requestEntity = null;
        String token = "";
        try {
            requestEntity = RequestEntity.post(new URL(appConfig.getTokenUrl()).toURI()).contentType(MediaType.APPLICATION_JSON).body(tokenJsonBody());
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String body = responseEntity.getBody();
                token = body.substring(1, body.lastIndexOf("\""));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return token;
    }

    private String tokenJsonBody() {
        return "{\n" +
                "    \"Username\": \"apiuser\",\n" +
                "    \"Password\": \"boD/8hX1EQt\",\n" +
                "    \"ApiKey\": \"LegalZoom\"\n" +
                "}";
    }

    private String callContactIdApi(String token) throws MalformedURLException, URISyntaxException {
        String contactIdResponse = "";
        RequestEntity<String> requestEntity = RequestEntity.post(new URL(appConfig.getTokenUrl()).toURI()).contentType(MediaType.APPLICATION_JSON).body(tokenJsonBody());
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
        return contactIdResponse;
    }

    private List<Long> extractContactIds(String contactIdResponse) {
        List<Long> contactIdsList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(contactIdResponse);
        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) object;
            JSONObject recordInforObject = jsonObject.getJSONObject("RecordInfo");
            Long id = recordInforObject.getLong("Id");
            contactIdsList.add(id);
        }
        return contactIdsList;
    }

    private ResponseEntity<Resource> callAudioApiOld(String url, String jsonToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.valueOf("audio/wav")));
        headers.set("Authorization", "JWT " + jsonToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> response = null;
        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Resource.class);
        } catch (Exception e) {

        }
        return response;
    }

    private ResponseEntity<Resource> callAudioApi(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.valueOf("audio/wav")));
        headers.set("Authorization", "JWT " + mainJsonToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> response = null;
        String url = "https://feapif.callminer.net/api/v2/downloadaudio/" + id + "/wav";
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, Resource.class);
        } catch (Exception e) {
            System.out.println("****************Sashi Token refreshed********************");
            mainJsonToken = getToken();
            response = callAudioApi(id);
        }
        return response;
    }

    private void downloadAudio(String jsonToken) throws MalformedURLException, URISyntaxException {
        List<String> idsList = csvUtil.getIds(appConfig.getCsvFilePath());
        System.out.println("Ids size : " + idsList.size());
        Long begTime = System.currentTimeMillis();

        for (String id : idsList) {
            performApiCallTaskOld(jsonToken, id);
        }
        Long endTime = System.currentTimeMillis();
        System.out.println("Execution time for all files is : " + (endTime - begTime));
    }

    private String performApiCallTaskOld(String jsonToken, String id) {
        String url = "https://feapif.callminer.net/api/v2/downloadaudio/" + id + "/wav";
        //System.out.println("url : " + url);
        ResponseEntity<Resource> response = callAudioApiOld(url, jsonToken);
        if (!response.getStatusCode().is2xxSuccessful()) {
            jsonToken = tokenJsonBody();
            callAudioApiOld(url, jsonToken);
        }
        contentManagementService.uploadFile(appConfig.getBucketName(), "CallRecordings1/" + id, response.getBody());
        return id;
    }

    private String performApiCallTask(String id, String csvFileNameToCreateFolderInBucket) {
        //System.out.println("url : " + url);
        mainJsonToken = getToken();
        ResponseEntity<Resource> response = callAudioApi(id);
        //if (!response.getStatusCode().is2xxSuccessful()) {
        //System.out.println("Shashi new token request : ");
        //jsonToken = tokenJsonBody();
        //callAudioApi(url, jsonToken);
        //}
        System.out.println("Uploading file for : "+(csvFileNameToCreateFolderInBucket + "/" + id));
        contentManagementService.uploadFile(appConfig.getBucketName(), "CallRecordings/" + csvFileNameToCreateFolderInBucket + "/" + id, response.getBody());
        return id;
    }

    private void downloadAudioThreadPool() throws MalformedURLException, URISyntaxException {
        String filePath = appConfig.getCsvFilePath();
        File file = new File(filePath);
        File[] csvFiles = file.listFiles();
        Long mainBegTime = System.currentTimeMillis();
        for (File eachCsvFile : csvFiles) {
            List<String> idsList = csvUtil.getIds(eachCsvFile.getAbsolutePath());
            System.out.println("CSV File Name : " + eachCsvFile.getName());
            String originalCSVFileName = eachCsvFile.getAbsoluteFile().getName();
            if (originalCSVFileName.contains(".csv")) {
                String csvFileNameToCreateFolderInBucket = originalCSVFileName.substring(0, originalCSVFileName.lastIndexOf(".csv"));
                System.out.println("Ids size : " + idsList.size());
                Long begTime = System.currentTimeMillis();

                List<Callable<String>> tasks = new ArrayList<>();
                for (String id : idsList) {
                    Callable task = () -> {
                        // System.out.println(id + "-------Thread-------" + Thread.currentThread().getName());
                        return performApiCallTask(id, csvFileNameToCreateFolderInBucket);
                    };
                    tasks.add(task);
                }
                try {
                    List<Future<String>> futures = executorService.invokeAll(tasks);
                    for (Future future : futures) {
                        String result = (String) future.get();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } finally {
                    executorService.shutdown();
                }
                Long endTime = System.currentTimeMillis();
                System.out.println("For each CSV Execution time : " + (endTime - begTime));
            }
        }
        Long mainEndTime = System.currentTimeMillis();
        System.out.println("All the CSV Files Execution time : " + (mainEndTime - mainBegTime));
    }
}