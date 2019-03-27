package com.gorolykmaxim.homemediaapp.qbittorrent;

import com.gorolykmaxim.homemediaapp.model.torrent.query.DownloadingTorrent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

public class QbittorrentServiceTest {

    private RestTemplate restTemplate;
    private QbittorrentAuthorization authorization;
    private QbittorrentFactory factory;
    private URI baseUri;
    private QbittorrentService service;
    private String magnetLink, downloadFolder, id;

    @Before
    public void setUp() throws Exception {
        magnetLink = "magnet uri";
        downloadFolder = "/downloads/";
        id = UUID.randomUUID().toString();
        restTemplate = Mockito.mock(RestTemplate.class);
        authorization = Mockito.mock(QbittorrentAuthorization.class);
        factory = Mockito.mock(QbittorrentFactory.class);
        baseUri = URI.create("http://localhost:8080");
        service = new QbittorrentService(restTemplate, authorization, factory, baseUri);
    }

    @Test
    public void downloadViaMagnetLink() {
        HttpEntity<MultiValueMap<String, String>> expectedRequest = createDownloadRequest();
        service.downloadViaMagnetLink(magnetLink, downloadFolder);
        Mockito.verify(authorization).applyTo(expectedRequest.getHeaders());
        Mockito.verify(restTemplate).postForLocation(baseUri.resolve("/command/download"), expectedRequest);
    }

    @Test
    public void renewAuthorizationAndDownloadViaMagnetLink() {
        URI expectedUri = baseUri.resolve("/command/download");
        HttpEntity<MultiValueMap<String, String>> expectedRequest = createDownloadRequest();
        Mockito.when(restTemplate.postForLocation(expectedUri, expectedRequest))
                .thenAnswer(new ForbiddenThenSuccessAnswer());
        service.downloadViaMagnetLink(magnetLink, downloadFolder);
        InOrder order = Mockito.inOrder(authorization, restTemplate);
        // Try to use existing SID for authentication.
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        // Try to create torrent and fail.
        order.verify(restTemplate).postForLocation(expectedUri, expectedRequest);
        // Renew authorization.
        order.verify(authorization).renew();
        // Apply newly obtained SID to a new download request.
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        // Try to create torrent second time.
        order.verify(restTemplate).postForLocation(expectedUri, expectedRequest);
    }

    @Test(expected = QbittorrentService.DownloadTorrentError.class)
    public void failToDownloadTorrent() {
        Mockito.when(restTemplate.postForLocation(baseUri.resolve("/command/download"), createDownloadRequest()))
                .thenThrow(Mockito.mock(RuntimeException.class));
        service.downloadViaMagnetLink(magnetLink, downloadFolder);
    }

    @Test
    public void deleteTorrentById() {
        HttpEntity<MultiValueMap<String, String>> expectedRequest = createDeleteRequest();
        service.deleteTorrentById(id);
        Mockito.verify(authorization).applyTo(expectedRequest.getHeaders());
        Mockito.verify(restTemplate).postForLocation(baseUri.resolve("/command/delete"), expectedRequest);
    }

    @Test
    public void renewAuthorizationAndDeleteTorrentById() {
        URI expectedUri = baseUri.resolve("/command/delete");
        HttpEntity<MultiValueMap<String, String>> expectedRequest = createDeleteRequest();
        Mockito.when(restTemplate.postForLocation(expectedUri, expectedRequest))
                .thenAnswer(new ForbiddenThenSuccessAnswer());
        service.deleteTorrentById(id);
        InOrder order = Mockito.inOrder(authorization, restTemplate);
        // Try to use existing SID for authentication.
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        // Try to create torrent and fail.
        order.verify(restTemplate).postForLocation(expectedUri, expectedRequest);
        // Renew authorization.
        order.verify(authorization).renew();
        // Apply newly obtained SID to a new download request.
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        // Try to create torrent second time.
        order.verify(restTemplate).postForLocation(expectedUri, expectedRequest);
    }

    @Test(expected = QbittorrentService.DeleteTorrentError.class)
    public void failToDeleteTorrentById() {
        Mockito.when(restTemplate.postForLocation(baseUri.resolve("/command/delete"), createDeleteRequest()))
                .thenThrow(Mockito.mock(RuntimeException.class));
        service.deleteTorrentById(id);
    }

    @Test
    public void findAll() {
        Qbittorrent expectedTorrent = Mockito.mock(Qbittorrent.class);
        ResponseEntity<List<Map<String, String>>> expectedResponse = createFindAllResponse(false);
        HttpEntity expectedRequest = createFindAllRequest();
        Mockito.when(restTemplate.exchange(baseUri.resolve("/query/torrents").toString(), HttpMethod.GET,
                expectedRequest, new ParameterizedTypeReference<List<Map<String, String>>>() {},
                createFindAllUriParameters()))
                .thenReturn(expectedResponse);
        Mockito.when(factory.create(expectedResponse.getBody().get(0))).thenReturn(expectedTorrent);
        List<DownloadingTorrent> torrents = service.findAll();
        Mockito.verify(authorization).applyTo(expectedRequest.getHeaders());
        Assert.assertEquals(Collections.singletonList(expectedTorrent), torrents);
    }

    @Test
    public void renewAuthorizationAndFindAll() {
        Qbittorrent expectedTorrent = Mockito.mock(Qbittorrent.class);
        ResponseEntity<List<Map<String, String>>> expectedResponse = createFindAllResponse(false);
        HttpEntity expectedRequest = createFindAllRequest();
        Mockito.when(restTemplate.exchange(baseUri.resolve("/query/torrents").toString(), HttpMethod.GET,
                expectedRequest, new ParameterizedTypeReference<List<Map<String, String>>>() {},
                createFindAllUriParameters()))
                .thenAnswer(new ForbiddenThenReturnTorrentsAnswer(expectedResponse));
        Mockito.when(factory.create(expectedResponse.getBody().get(0))).thenReturn(expectedTorrent);
        List<DownloadingTorrent> torrents = service.findAll();
        InOrder order = Mockito.inOrder(authorization);
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        order.verify(authorization).renew();
        order.verify(authorization).applyTo(expectedRequest.getHeaders());
        Assert.assertEquals(Collections.singletonList(expectedTorrent), torrents);
    }

    @Test(expected = QbittorrentService.GetTorrentsError.class)
    public void failToFindAllDueToEmptyBody() {
        ResponseEntity<List<Map<String, String>>> expectedResponse = createFindAllResponse(true);
        HttpEntity expectedRequest = createFindAllRequest();
        Mockito.when(restTemplate.exchange(baseUri.resolve("/query/torrents").toString(), HttpMethod.GET,
                expectedRequest, new ParameterizedTypeReference<List<Map<String, String>>>() {},
                createFindAllUriParameters()))
                .thenReturn(expectedResponse);
        service.findAll();
    }

    @Test(expected = QbittorrentService.GetTorrentsError.class)
    public void failToFindAll() {
        HttpEntity expectedRequest = createFindAllRequest();
        Mockito.when(restTemplate.exchange(baseUri.resolve("/query/torrents").toString(), HttpMethod.GET,
                expectedRequest, new ParameterizedTypeReference<List<Map<String, String>>>() {},
                createFindAllUriParameters()))
                .thenThrow(Mockito.mock(RuntimeException.class));
        service.findAll();
    }

    private HttpEntity<MultiValueMap<String, String>> createDownloadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("urls", magnetLink);
        body.add("savepath", downloadFolder);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<MultiValueMap<String, String>> createDeleteRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("hashes", id);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity createFindAllRequest() {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity(headers);
    }

    private Map<String, String> createFindAllUriParameters() {
        Map<String, String> uriParameters = new HashMap<>();
        uriParameters.put("sort", "progress");
        return uriParameters;
    }

    private ResponseEntity<List<Map<String, String>>> createFindAllResponse(boolean emptyBody) {
        if (emptyBody) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            Map<String, String> rawQbittorrent = Collections.emptyMap();
            return new ResponseEntity<>(Collections.singletonList(rawQbittorrent), HttpStatus.OK);
        }
    }

    private class ForbiddenThenSuccessAnswer implements Answer<URI> {

        private boolean isFirstCall = true;

        @Override
        public URI answer(InvocationOnMock invocation) throws Throwable {
            if (isFirstCall) {
                // Emulate a '403 Forbidden' response from qbittorrent due to expired SID.
                isFirstCall = false;
                throw Mockito.mock(HttpClientErrorException.Forbidden.class);
            } else {
                // Successfully process the second attempt since it should be made with a new SID.
                return null;
            }
        }
    }

    private class ForbiddenThenReturnTorrentsAnswer implements Answer<ResponseEntity<List<Map<String, String>>>> {

        private boolean isFirstCall = true;
        private ResponseEntity<List<Map<String, String>>> response;

        public ForbiddenThenReturnTorrentsAnswer(ResponseEntity<List<Map<String, String>>> response) {
            this.response = response;
        }

        @Override
        public ResponseEntity<List<Map<String, String>>> answer(InvocationOnMock invocation) throws Throwable {
            if (isFirstCall) {
                // Emulate a '403 Forbidden' response from qbittorrent due to expired SID.
                isFirstCall = false;
                throw Mockito.mock(HttpClientErrorException.Forbidden.class);
            } else {
                // Successfully process the second attempt since it should be made with a new SID.
                return response;
            }
        }
    }

}