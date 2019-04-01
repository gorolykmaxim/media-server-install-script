package com.gorolykmaxim.homemediaapp.model.torrent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadingTorrentRepository {
    private TorrentService service;
    private String sort;
    private boolean reverse;

    public DownloadingTorrentRepository(TorrentService service, String sort, boolean reverse) {
        this.service = service;
        this.sort = sort;
        this.reverse = reverse;
    }

    public List<DownloadingTorrent> findAll() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sort", sort);
        parameters.put("reverse", Boolean.toString(reverse));
        return service.find(parameters);
    }

    public List<DownloadingTorrent> findDownloading() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sort", sort);
        parameters.put("reverse", Boolean.toString(reverse));
        parameters.put("filter", "downloading");
        return service.find(parameters);
    }
}
